#!/usr/bin/env python3
"""Offline body-model integrity verifier; standard library only."""
from __future__ import annotations
import hashlib
import json
import struct
import sys
from pathlib import Path

COMPONENT_BYTES = {5120: 1, 5121: 1, 5122: 2, 5123: 2, 5125: 4, 5126: 4}
TYPE_COMPONENTS = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT2": 4, "MAT3": 9, "MAT4": 16}
INDEX_TYPES = {5121, 5123, 5125}

def integer(value, label):
    if isinstance(value, bool) or not isinstance(value, int):
        raise ValueError(f"{label} is not an integer")
    return value

def inspect_glb(data: bytes, entry: dict) -> dict:
    if len(data) < 20:
        raise ValueError("GLB header is truncated")
    magic, version, length = struct.unpack_from("<4sII", data, 0)
    if magic != b"glTF": raise ValueError("GLB magic is invalid")
    if version != 2: raise ValueError("GLB version is unsupported")
    if length != len(data): raise ValueError("GLB length does not match the asset")
    cursor, json_chunk, bin_chunk = 12, None, None
    while cursor < len(data):
        if len(data) - cursor < 8: raise ValueError("GLB chunk header is truncated")
        size, kind = struct.unpack_from("<II", data, cursor); cursor += 8
        if size > len(data) - cursor: raise ValueError("GLB chunk is truncated")
        chunk = data[cursor:cursor + size]; cursor += size
        if kind == 0x4E4F534A:
            if json_chunk is not None: raise ValueError("GLB contains duplicate JSON chunks")
            json_chunk = chunk
        elif kind == 0x004E4942:
            if bin_chunk is not None: raise ValueError("GLB contains duplicate BIN chunks")
            bin_chunk = chunk
    if json_chunk is None: raise ValueError("GLB JSON chunk is missing")
    if bin_chunk is None: raise ValueError("GLB BIN chunk is missing")
    try: document = json.loads(json_chunk.rstrip(b"\0 ").decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc: raise ValueError(f"GLB JSON is invalid: {exc}") from exc
    arrays = {key: document.get(key) for key in ("buffers", "bufferViews", "accessors", "nodes", "meshes", "materials")}
    if not all(isinstance(value, list) for value in arrays.values()): raise ValueError("GLB required arrays are missing")
    buffers, views, accessors, nodes, meshes, materials = (arrays[key] for key in arrays)
    if len(buffers) != 1: raise ValueError("GLB must contain exactly one buffer")
    if integer(buffers[0].get("byteLength"), "buffer byteLength") != len(bin_chunk): raise ValueError("GLB buffer byteLength does not match BIN chunk")
    for index, view in enumerate(views):
        if integer(view.get("buffer"), f"bufferView[{index}] buffer") != 0: raise ValueError(f"bufferView[{index}] references an invalid buffer")
        offset, size = integer(view.get("byteOffset", 0), "bufferView byteOffset"), integer(view.get("byteLength"), "bufferView byteLength")
        if offset < 0 or size < 0 or offset + size > len(bin_chunk): raise ValueError(f"bufferView[{index}] exceeds the BIN buffer")
        if "byteStride" in view and (integer(view["byteStride"], "byteStride") < 4 or integer(view["byteStride"], "byteStride") > 252 or integer(view["byteStride"], "byteStride") % 4): raise ValueError(f"bufferView[{index}] has an invalid byteStride")
    info = []
    for index, accessor in enumerate(accessors):
        view_index = integer(accessor.get("bufferView"), "accessor bufferView")
        if not 0 <= view_index < len(views): raise ValueError(f"accessor[{index}] references an invalid bufferView")
        component_type = integer(accessor.get("componentType"), "componentType")
        component_size = COMPONENT_BYTES.get(component_type)
        components = TYPE_COMPONENTS.get(accessor.get("type"))
        if component_size is None or components is None: raise ValueError(f"accessor[{index}] has unsupported component metadata")
        count = integer(accessor.get("count"), "accessor count")
        view = views[view_index]; offset = integer(view.get("byteOffset", 0), "view offset") + integer(accessor.get("byteOffset", 0), "accessor offset")
        stride = integer(view.get("byteStride", component_size * components), "accessor stride")
        if count < 0 or stride < component_size * components: raise ValueError(f"accessor[{index}] has invalid count or stride")
        size = 0 if count == 0 else (count - 1) * stride + component_size * components
        view_offset = integer(view.get("byteOffset", 0), "view offset")
        view_size = integer(view.get("byteLength"), "view length")
        if offset < 0 or offset + size > len(bin_chunk) or offset + size > view_offset + view_size: raise ValueError(f"accessor[{index}] exceeds its bufferView or the BIN buffer")
        info.append((component_type, accessor.get("type"), count, offset, stride))
    names = []
    for index, node in enumerate(nodes):
        if "mesh" in node and not 0 <= integer(node["mesh"], "node mesh") < len(meshes): raise ValueError(f"node[{index}] references an invalid mesh")
        if not isinstance(node.get("name"), str) or not node["name"]: raise ValueError(f"node[{index}] is missing name")
        names.append(node["name"])
    triangles = 0
    for mesh_index, mesh in enumerate(meshes):
        primitives = mesh.get("primitives")
        if not isinstance(primitives, list) or not primitives: raise ValueError(f"mesh[{mesh_index}] primitives are missing")
        for primitive in primitives:
            attributes = primitive.get("attributes")
            if not isinstance(attributes, dict) or "POSITION" not in attributes: raise ValueError("primitive POSITION is missing")
            position = integer(attributes["POSITION"], "POSITION accessor")
            if not 0 <= position < len(info): raise ValueError("POSITION accessor is out of bounds")
            if any(not 0 <= integer(value, "attribute accessor") < len(info) for value in attributes.values()): raise ValueError("attribute accessor is out of bounds")
            indices = integer(primitive.get("indices"), "indices accessor")
            if not 0 <= indices < len(info): raise ValueError("indices accessor is out of bounds")
            component_type, accessor_type, count, offset, stride = info[indices]
            if component_type not in INDEX_TYPES or accessor_type != "SCALAR": raise ValueError("invalid index accessor")
            if count % 3: raise ValueError("index count is not triangular")
            material = integer(primitive.get("material"), "material")
            if not 0 <= material < len(materials): raise ValueError("primitive material is out of bounds")
            for item in range(count):
                at = offset + item * stride
                value = bin_chunk[at] if component_type == 5121 else struct.unpack_from("<H" if component_type == 5123 else "<I", bin_chunk, at)[0]
                if not 0 <= value < info[position][2]: raise ValueError("index accessor contains an out-of-bounds index")
            triangles += count // 3
    for key in ("meshes", "materials", "triangles", "selectableMeshes"):
        if integer(entry.get(key), key) <= 0: raise ValueError(f"{entry['gender']} required metrics must be positive")
    region_ids = [node.get("extras", {}).get("region_id") for node in nodes if isinstance(node.get("extras"), dict) and isinstance(node.get("extras", {}).get("region_id"), str)]
    selectable_count = sum(node.get("extras", {}).get("selectable") is True for node in nodes if isinstance(node.get("extras"), dict))
    regions = entry.get("requiredRegions")
    if not isinstance(regions, list) or not regions: raise ValueError(f"{entry['gender']} required regions are missing")
    present = [region for region in regions if any(region.lower() == region_id.lower() for region_id in region_ids)]
    selectable = selectable_count
    metrics = {"gender": entry["gender"], "assetPath": entry["assetPath"], "bytes": len(data), "meshes": len(meshes), "materials": len(materials), "triangles": triangles, "selectableMeshes": selectable, "requiredRegions": present, "nodeNames": names}
    for key in ("meshes", "materials", "triangles", "selectableMeshes"):
        if entry[key] != metrics[key]: raise ValueError(f"{entry['gender']} {key} mismatch: expected {entry[key]}, got {metrics[key]}")
    missing = sorted(set(regions) - set(present))
    if missing: raise ValueError(f"{entry['gender']} required regions missing: {', '.join(missing)}")
    return metrics

def blocked(value) -> bool:
    return not isinstance(value, str) or value.strip().lower() in {"", "missing", "blocked", "not supplied", "not confirmed"}

def main(repo: Path) -> int:
    report = {"valid": False, "runtimeValid": False, "structureValid": False, "errors": [], "modelCount": 0, "totalBytes": 0, "models": [], "licenseStatus": "blocked", "authorization": {"licenseStatus": "blocked", "apkRedistribution": "blocked", "releaseGate": "blocked"}}
    try: manifest = json.loads((repo / "app/src/main/assets/body/model-manifest.json").read_text()); entries = manifest["entries"]
    except Exception as exc: report["errors"] = [f"manifest file is missing or invalid: {exc}"]; print(json.dumps(report, indent=2)); return 1
    report["modelCount"] = len(entries); technical_ok = True; authorization_ok = True; genders = []
    for entry in entries:
        gender, path = entry.get("gender"), entry.get("assetPath"); genders.append(gender)
        if gender not in {"MALE", "FEMALE"} or genders.count(gender) > 1: report["errors"].append(f"entry {gender} gender is invalid or duplicated"); technical_ok = False
        if not isinstance(path, str) or not path or path.startswith("/") or "\\" in path or ".." in Path(path).parts or ":" in path.split("/")[0]: report["errors"].append(f"entry {gender} assetPath is not relative"); technical_ok = False; continue
        auth_error = entry.get("licenseStatus") != "confirmed" or blocked(entry.get("sourceUrlOrRepository")) or blocked(entry.get("sourceCommitOrVersion")) or blocked(entry.get("license")) or blocked(entry.get("attribution")) or entry.get("apkRedistributionAuthorization") != "confirmed"
        if auth_error:
            authorization_ok = False
            if entry.get("licenseStatus") != "confirmed": report["errors"].append(f"licenseStatus is not confirmed: {gender}")
            if blocked(entry.get("sourceUrlOrRepository")): report["errors"].append(f"source URL or repository is missing or blocked: {gender}")
            if blocked(entry.get("sourceCommitOrVersion")): report["errors"].append(f"source commit or version is missing or blocked: {gender}")
            if entry.get("apkRedistributionAuthorization") != "confirmed": report["errors"].append(f"APK redistribution authorization is not confirmed: {gender}")
        file = repo / "app/src/main/assets" / path
        if not file.is_file(): report["errors"].append(f"asset does not exist: {path}"); technical_ok = False; continue
        data = file.read_bytes(); report["totalBytes"] += len(data)
        if integer(entry.get("bytes"), "bytes") != len(data): report["errors"].append(f"byte count mismatch: {path}"); technical_ok = False
        if not isinstance(entry.get("sha256"), str) or hashlib.sha256(data).hexdigest() != entry["sha256"].lower(): report["errors"].append(f"SHA-256 mismatch: {path}"); technical_ok = False
        try: report["models"].append(inspect_glb(data, entry))
        except Exception as exc: report["errors"].append(f"entry {gender} GLB structure is invalid: {exc}"); technical_ok = False
    for gender in ("MALE", "FEMALE"):
        if gender not in genders: report["errors"].append(f"missing gender: {gender}"); technical_ok = False
    report["runtimeValid"] = technical_ok; report["structureValid"] = technical_ok; report["licenseStatus"] = "confirmed" if authorization_ok else "blocked"
    report["authorization"] = {"licenseStatus": report["licenseStatus"], "apkRedistribution": "confirmed" if authorization_ok else "blocked", "releaseGate": "authorized" if authorization_ok else "blocked"}
    report["valid"] = technical_ok and authorization_ok and not report["errors"]
    print(json.dumps(report, indent=2)); return 0 if report["valid"] else max(1, len(report["errors"]))

if __name__ == "__main__": raise SystemExit(main(Path(sys.argv[1] if len(sys.argv) > 1 else Path(__file__).resolve().parents[1])))

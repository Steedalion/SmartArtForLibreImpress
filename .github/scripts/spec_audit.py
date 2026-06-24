#!/usr/bin/env python3
"""
Nightly spec audit for SmartArt for LibreOffice Impress.

For each layout/parser class:
  1. Reconstruct it from spec docs only (Claude Haiku).
  2. Compare the reconstruction to the actual source (Claude Haiku).
  3. Write a dated Markdown report to spec-audit/YYYY-MM-DD.md.

Run via GitHub Actions (see .github/workflows/spec-audit.yml).
Requires: ANTHROPIC_API_KEY env var, `pip install anthropic`.
"""

import json
import os
import sys
from datetime import datetime, timezone

import anthropic

# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------
REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
LAYOUT = "src/main/java/org/libreimpress/smartart/layout"
PARSER = "src/main/java/org/libreimpress/smartart/parsers"

# Data-model files the reconstructor may read (type info only, not logic).
MODEL_PATHS = [
    f"{LAYOUT}/DiagramLayout.java",
    f"{LAYOUT}/LaidOutShape.java",
    f"{LAYOUT}/Edge.java",
    f"{LAYOUT}/ShapeKind.java",
    "src/main/java/org/libreimpress/smartart/models/DiagramNode.java",
    "src/main/java/org/libreimpress/smartart/models/DiagramType.java",
]

COMPONENTS = [
    {
        "name": "HierarchyLayout",
        "src": f"{LAYOUT}/HierarchyLayout.java",
        "specs": [
            "impressSmartArt.md",
            "Phase4_ImplementationPlan.md",
            "Phase6_ImplementationPlan.md",
        ],
    },
    {
        "name": "HubAndSpokeLayout",
        "src": f"{LAYOUT}/HubAndSpokeLayout.java",
        "specs": ["impressSmartArt.md", "Phase8_ImplementationPlan.md"],
    },
    {
        "name": "ProcessFlowLayout",
        "src": f"{LAYOUT}/ProcessFlowLayout.java",
        "specs": ["impressSmartArt.md", "Phase7_ImplementationPlan.md"],
    },
    {
        "name": "SequentialChevronLayout",
        "src": f"{LAYOUT}/SequentialChevronLayout.java",
        "specs": ["impressSmartArt.md", "Phase9_ImplementationPlan.md"],
    },
    {
        "name": "CycleLayout",
        "src": f"{LAYOUT}/CycleLayout.java",
        "specs": ["impressSmartArt.md", "Phase11_ImplementationPlan.md"],
    },
    {
        "name": "CycleArrowLayout",
        "src": f"{LAYOUT}/CycleArrowLayout.java",
        "specs": ["impressSmartArt.md", "Phase11_ImplementationPlan.md"],
    },
    {
        "name": "HierarchyParser",
        "src": f"{PARSER}/HierarchyParser.java",
        "specs": ["impressSmartArt.md", "Phase3_ImplementationPlan.md"],
    },
]

MODEL = "claude-haiku-4-5-20251001"


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def read(rel_path: str) -> str:
    with open(os.path.join(REPO, rel_path), encoding="utf-8") as f:
        return f.read()


def call_haiku(prompt: str, tool_name: str, tool_schema: dict) -> dict:
    """Call Claude Haiku and force structured output via tool use."""
    client = anthropic.Anthropic()
    tools = [
        {
            "name": tool_name,
            "description": "Submit your structured findings.",
            "input_schema": tool_schema,
        }
    ]
    response = client.messages.create(
        model=MODEL,
        max_tokens=4096,
        tools=tools,
        tool_choice={"type": "any"},
        messages=[{"role": "user", "content": prompt}],
    )
    for block in response.content:
        if block.type == "tool_use" and block.name == tool_name:
            return block.input
    return {}


# ---------------------------------------------------------------------------
# Phase 1: Reconstruct
# ---------------------------------------------------------------------------
RECO_SCHEMA = {
    "type": "object",
    "properties": {
        "code": {
            "type": "string",
            "description": "Full compilable Java source for the class",
        },
        "assumptions": {
            "type": "array",
            "items": {"type": "string"},
            "description": "Things you guessed because the spec was silent",
        },
        "ambiguities": {
            "type": "array",
            "items": {"type": "string"},
            "description": "Parts of the spec that were unclear or conflicting",
        },
        "missing": {
            "type": "array",
            "items": {"type": "string"},
            "description": "Things the spec never mentioned but you needed",
        },
    },
    "required": ["code", "assumptions", "ambiguities", "missing"],
}


def reconstruct(comp: dict) -> dict:
    spec_text = "\n\n".join(
        f"=== {s} ===\n{read(s)}" for s in comp["specs"]
    )
    model_text = "\n\n".join(
        f"=== {m} ===\n{read(m)}" for m in MODEL_PATHS
    )

    prompt = f"""You are running a spec-fidelity audit of the SmartArt for LibreOffice Impress project.

TASK: Reconstruct {comp["name"]} from scratch using ONLY the spec documents below.
Do NOT use knowledge of the actual implementation — pretend you have never seen it.

CONSTRAINT: The class must compile against the data-model types provided.
Units throughout the project: 1/100 mm (matching the LibreOffice UNO API).

SPEC DOCUMENTS:
{spec_text}

DATA MODEL FILES (understand the types; do not treat them as implementation hints):
{model_text}

Write the full {comp["name"]} Java class.
Then be completely honest about:
- assumptions: things you guessed because the spec was silent
- ambiguities: things the spec said but was unclear about
- missing: things you needed that the spec never mentioned at all

The goal is to improve the spec, not to write perfect code."""

    return call_haiku(prompt, "submit_reconstruction", RECO_SCHEMA)


# ---------------------------------------------------------------------------
# Phase 2: Compare
# ---------------------------------------------------------------------------
GAP_SCHEMA = {
    "type": "object",
    "properties": {
        "gaps": {
            "type": "array",
            "items": {"type": "string"},
            "description": (
                "Specific things the spec omits that the real code relies on. "
                "Be concrete: name constants, algorithms, edge cases."
            ),
        },
        "suggestions": {
            "type": "array",
            "items": {"type": "string"},
            "description": (
                "Copy-paste ready sentences to add to a spec file. "
                "Format each as: 'PhaseN_ImplementationPlan.md: <sentence to add>.'"
            ),
        },
        "divergences": {
            "type": "array",
            "items": {"type": "string"},
            "description": (
                "Major algorithmic differences between reconstruction and actual code, "
                "one-line reason for each."
            ),
        },
    },
    "required": ["gaps", "suggestions", "divergences"],
}


def compare(comp: dict, reco: dict) -> dict:
    actual = read(comp["src"])

    prompt = f"""You are comparing a spec-based reconstruction of {comp["name"]} to the actual implementation.

RECONSTRUCTION NOTES:
- Assumptions: {json.dumps(reco.get("assumptions", []), indent=2)}
- Ambiguities: {json.dumps(reco.get("ambiguities", []), indent=2)}
- Missing from spec: {json.dumps(reco.get("missing", []), indent=2)}

RECONSTRUCTED CODE:
```java
{reco.get("code", "(reconstruction failed)")}
```

ACTUAL CODE:
```java
{actual}
```

For every significant difference between reconstruction and actual code, decide:
"Should the spec have mentioned this?"

GOOD finding: "HierarchyLayout uses CHILD_GAP=600 (1/100 mm) between siblings — \
the spec mentions spacing but gives no value."
BAD finding: "The spec is vague." (too vague to act on)

Return:
- gaps: specific spec omissions the real code relies on
- suggestions: exact sentences to add to a named spec file
- divergences: algorithmic differences with a one-line reason"""

    return call_haiku(prompt, "submit_gap_analysis", GAP_SCHEMA)


# ---------------------------------------------------------------------------
# Phase 3: Report
# ---------------------------------------------------------------------------
def write_report(date: str, findings: list[dict]) -> None:
    lines = [
        f"# Spec Audit — {date}",
        "",
        "## Summary",
        "",
    ]

    total_gaps = sum(len(f.get("gaps", [])) for f in findings)
    with_gaps = sum(1 for f in findings if f.get("gaps"))
    lines.append(
        f"{len(findings)} components audited; {with_gaps} had spec gaps "
        f"({total_gaps} total findings). "
        "Common themes: missing numeric constants, undocumented edge cases for "
        "empty/single-node input, and algorithm choices left implicit in the spec."
    )
    lines.append("")
    lines.append("## Component Findings")

    for f in findings:
        name = f["component"]
        comp = next(c for c in COMPONENTS if c["name"] == name)
        lines += [
            "",
            f"### {name}",
            f"**Source:** `{comp['src']}`  ",
            f"**Spec files:** {', '.join(comp['specs'])}",
            "",
            "#### Gaps",
        ]
        for g in f.get("gaps", []) or ["(none found)"]:
            lines.append(f"- {g}")

        lines += ["", "#### Suggested Spec Additions"]
        for s in f.get("suggestions", []) or ["(none)"]:
            lines.append(f"- {s}")

        lines += ["", "#### Reconstruction vs. Real Code"]
        for d in f.get("divergences", []) or ["(no significant divergences)"]:
            lines.append(f"- {d}")

    lines += [
        "",
        "---",
        "",
        "## Priority Improvements",
        "",
        "Ranked by breadth of impact across components:",
        "",
    ]
    # Collect all suggestions and pick the most-mentioned themes.
    all_suggestions = [
        s for f in findings for s in f.get("suggestions", [])
    ]
    for i, s in enumerate(all_suggestions[:5], 1):
        lines.append(f"{i}. {s}")

    lines += [
        "",
        "---",
        f"_Generated by spec-audit — {date}_",
    ]

    out_dir = os.path.join(REPO, "spec-audit")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, f"{date}.md")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    print(f"Report written to spec-audit/{date}.md")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main() -> None:
    # Fail fast on missing credentials: without this guard a total auth
    # failure still produces a "report" full of SDK error strings, which the
    # workflow then commits and pushes (see the dropped 2026-06-1*.md reports).
    if not os.environ.get("ANTHROPIC_API_KEY"):
        sys.exit(
            "ANTHROPIC_API_KEY is not set — aborting before any report is "
            "written. Set the secret in the repo's Actions settings."
        )

    date = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    findings = []
    failures = 0

    for comp in COMPONENTS:
        print(f"[{comp['name']}] reconstructing…", flush=True)
        try:
            reco = reconstruct(comp)
        except anthropic.AuthenticationError as e:
            sys.exit(f"Authentication failed ({e}) — aborting; no report written.")
        except Exception as e:
            print(f"  reconstruction failed: {e}", file=sys.stderr)
            reco = {"code": "", "assumptions": [], "ambiguities": [], "missing": [str(e)]}
            failures += 1

        print(f"[{comp['name']}] comparing…", flush=True)
        try:
            gaps = compare(comp, reco)
        except anthropic.AuthenticationError as e:
            sys.exit(f"Authentication failed ({e}) — aborting; no report written.")
        except Exception as e:
            print(f"  comparison failed: {e}", file=sys.stderr)
            gaps = {"gaps": [str(e)], "suggestions": [], "divergences": []}
            failures += 1

        gaps["component"] = comp["name"]
        findings.append(gaps)

    # If every call failed the report would be pure error noise — don't write
    # or commit it.
    if failures == 2 * len(COMPONENTS):
        sys.exit("All audit calls failed — aborting; no report written.")

    write_report(date, findings)


if __name__ == "__main__":
    main()

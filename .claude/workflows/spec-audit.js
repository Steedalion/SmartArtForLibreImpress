export const meta = {
  name: 'spec-audit',
  description: 'Reconstruct layout classes from spec only, compare to real code, write gap report',
  phases: [
    { title: 'Reconstruct', detail: 'Delete + rewrite each layout class from spec alone in a worktree' },
    { title: 'Compare',     detail: 'Diff reconstruction vs. actual code; classify spec gaps' },
    { title: 'Report',      detail: 'Write dated markdown report with actionable spec improvements' },
  ],
}

// --------------------------------------------------------------------------
// Components to audit: pure-Java, spec-driven classes only.
// Each agent deletes the file in an isolated worktree, reads only the listed
// spec docs + the shared data-model files, then rewrites from scratch.
// --------------------------------------------------------------------------
const PROJECT = '/home/csteed/Documents/SmartArtForLibreImpress'
const LAYOUT  = 'src/main/java/org/libreimpress/smartart/layout'
const PARSER  = 'src/main/java/org/libreimpress/smartart/parsers'

// Data-model files the reconstructor MAY read (they are fixtures, not spec).
const MODEL_FILES = [
  `${LAYOUT}/DiagramLayout.java`,
  `${LAYOUT}/LaidOutShape.java`,
  `${LAYOUT}/Edge.java`,
  `${LAYOUT}/ShapeKind.java`,
  'src/main/java/org/libreimpress/smartart/models/DiagramNode.java',
  'src/main/java/org/libreimpress/smartart/models/DiagramType.java',
].map(p => `${PROJECT}/${p}`).join(', ')

const COMPONENTS = [
  {
    name: 'HierarchyLayout',
    src:  `${LAYOUT}/HierarchyLayout.java`,
    specs: ['impressSmartArt.md', 'Phase4_ImplementationPlan.md', 'Phase6_ImplementationPlan.md'],
  },
  {
    name: 'HubAndSpokeLayout',
    src:  `${LAYOUT}/HubAndSpokeLayout.java`,
    specs: ['impressSmartArt.md', 'Phase8_ImplementationPlan.md'],
  },
  {
    name: 'ProcessFlowLayout',
    src:  `${LAYOUT}/ProcessFlowLayout.java`,
    specs: ['impressSmartArt.md', 'Phase7_ImplementationPlan.md'],
  },
  {
    name: 'SequentialChevronLayout',
    src:  `${LAYOUT}/SequentialChevronLayout.java`,
    specs: ['impressSmartArt.md', 'Phase9_ImplementationPlan.md'],
  },
  {
    name: 'CycleLayout',
    src:  `${LAYOUT}/CycleLayout.java`,
    specs: ['impressSmartArt.md', 'Phase11_ImplementationPlan.md'],
  },
  {
    name: 'CycleArrowLayout',
    src:  `${LAYOUT}/CycleArrowLayout.java`,
    specs: ['impressSmartArt.md', 'Phase11_ImplementationPlan.md'],
  },
  {
    name: 'HierarchyParser',
    src:  `${PARSER}/HierarchyParser.java`,
    specs: ['impressSmartArt.md', 'Phase3_ImplementationPlan.md'],
  },
]

const RECO_SCHEMA = {
  type: 'object',
  properties: {
    component:   { type: 'string', description: 'Class name reconstructed' },
    code:        { type: 'string', description: 'Full Java source you wrote' },
    assumptions: { type: 'array', items: { type: 'string' }, description: 'Things you assumed because the spec was silent' },
    ambiguities: { type: 'array', items: { type: 'string' }, description: 'Parts of the spec you found unclear or conflicting' },
    missing:     { type: 'array', items: { type: 'string' }, description: 'Things the spec never mentioned but you needed' },
  },
  required: ['component', 'code', 'assumptions', 'ambiguities', 'missing'],
}

const GAP_SCHEMA = {
  type: 'object',
  properties: {
    component:   { type: 'string' },
    gaps:        { type: 'array', items: { type: 'string' }, description: 'Specific missing or unclear spec items (be concrete: name constants, algorithms, edge cases)' },
    suggestions: { type: 'array', items: { type: 'string' }, description: 'Ready-to-paste sentences to add to a Phase*_ImplementationPlan.md file; prefix each with the target file name' },
    divergences: { type: 'array', items: { type: 'string' }, description: 'Where reconstruction differed from actual code and why' },
  },
  required: ['component', 'gaps', 'suggestions', 'divergences'],
}

// --------------------------------------------------------------------------
// Phase 1 – Reconstruct (parallel, each agent gets its own worktree)
// --------------------------------------------------------------------------
phase('Reconstruct')
log('Spawning ' + COMPONENTS.length + ' reconstruction agents (one worktree each)…')

const reconstructions = await parallel(COMPONENTS.map((comp, idx) => () =>
  agent(
    `SPEC-AUDIT RECONSTRUCTION TASK
================================
Project root: ${PROJECT}
Target class: ${comp.name}
Source file:  ${PROJECT}/${comp.src}

YOUR MISSION
------------
Reconstruct ${comp.name} from scratch using ONLY the spec documents listed below.
Do NOT read any other layout class or parser — only the spec files and the
shared data-model files described below.

PERMITTED READS
---------------
Spec files (in project root):
  ${comp.specs.map(s => `${PROJECT}/${s}`).join('\n  ')}

Shared data-model files (read these to understand the types — do not treat
them as implementation hints):
  ${MODEL_FILES}

FORBIDDEN READS
---------------
Do NOT read any other .java file in the layout/ or parsers/ package.

STEPS
-----
1. Read each spec file listed above.
2. Read the data-model files to understand available types.
3. Delete ${PROJECT}/${comp.src}
4. Write a clean, compilable Java implementation at ${PROJECT}/${comp.src}
   based solely on what the spec says (+ type signatures from the model files).
5. Return: the code you wrote, what you had to assume, what was ambiguous,
   and what the spec never mentioned but you needed.

BE HONEST about gaps — the purpose of this audit is to improve the spec, not
to produce perfect code. If you couldn't determine a constant value, say so.`,
    {
      isolation: 'worktree',
      schema: RECO_SCHEMA,
      label:  `reconstruct:${comp.name}`,
      phase:  'Reconstruct',
    }
  ).then(r => r ? { ...r, _src: comp.src, _specs: comp.specs } : null)
))

const validRecos = reconstructions.filter(Boolean)
log(`${validRecos.length}/${COMPONENTS.length} reconstructions completed.`)

// --------------------------------------------------------------------------
// Phase 2 – Compare (parallel; reads MAIN-repo source alongside reconstruction)
// --------------------------------------------------------------------------
phase('Compare')
log('Comparing reconstructions to actual source…')

const gaps = await parallel(validRecos.map(reco => () =>
  agent(
    `SPEC-AUDIT COMPARISON TASK
============================
Component: ${reco.component}
Spec files: ${reco._specs.map(s => `${PROJECT}/${s}`).join(', ')}
Actual source: ${PROJECT}/${reco._src}

RECONSTRUCTION NOTES
--------------------
Assumptions (things the agent guessed): ${JSON.stringify(reco.assumptions)}
Ambiguities (spec was unclear):         ${JSON.stringify(reco.ambiguities)}
Missing (never mentioned in spec):      ${JSON.stringify(reco.missing)}

RECONSTRUCTED CODE
------------------
\`\`\`java
${reco.code}
\`\`\`

YOUR TASK
---------
1. Read the ACTUAL implementation at ${PROJECT}/${reco._src}
2. Compare it line-by-line to the reconstruction above.
3. For every significant difference, judge: "Should the spec have said this?"
4. Formulate specific, actionable findings.

GOOD finding: "HierarchyLayout uses a CHILD_GAP constant of 600 (1/100 mm) between
siblings — the spec mentions that children are spaced out but gives no value."

BAD finding: "The spec is vague about spacing." (too vague to act on)

Return:
- gaps:        specific things the spec omits that the real code relies on
- suggestions: exact sentences to ADD to a named spec file (e.g.,
               "Phase6_ImplementationPlan.md: Add: 'Sibling gap = 600 (1/100 mm).'")
- divergences: major structural/algorithmic differences between reconstruction
               and actual code, with a one-line reason for each`,
    {
      schema: GAP_SCHEMA,
      label:  `compare:${reco.component}`,
      phase:  'Compare',
    }
  )
))

const validGaps = gaps.filter(Boolean)
log(`Gap analysis done: ${validGaps.length} components have findings.`)

// --------------------------------------------------------------------------
// Phase 3 – Report
// --------------------------------------------------------------------------
phase('Report')
const date = (args && args.date) ? args.date : 'undated'

await agent(
  `SPEC-AUDIT REPORT WRITER
=========================
Write the audit report to: ${PROJECT}/spec-audit/${date}.md

First run:
  mkdir -p ${PROJECT}/spec-audit

Then write the full Markdown file. Use this structure:

---
# Spec Audit — ${date}

## Summary
<One paragraph: N components audited, how many had gaps, and the top 2-3 themes
(e.g. "Missing numeric constants for spacing", "Edge cases around single-node
input not covered")>

## Component Findings

<For each component below, one section:>
### <ComponentName>
**Source:** \`<src path>\`
**Spec files checked:** <list>

#### Gaps
<Bullet list — concrete, specific>

#### Suggested Spec Additions
<Bullet list — copy-paste ready. Format: "**<FileName.md>:** <sentence to add.>">

#### Reconstruction vs. Real Code
<Bullet list of divergences>

---

## Priority Improvements
Top 5 spec improvements ranked by importance. One sentence each.

---
_Generated by spec-audit workflow — ${date}_

FINDINGS DATA (use this as the source of truth):
${JSON.stringify(validGaps, null, 2)}`,
  { label: 'write-report', phase: 'Report' }
)

log('Report written to spec-audit/' + date + '.md')
return { date, audited: COMPONENTS.length, completed: validGaps.length }

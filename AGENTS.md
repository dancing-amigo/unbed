# AGENTS.md

## Purpose
- This repository uses `.agent` as the working area for product and implementation planning.
- All contributors and agents should keep planning artifacts and progress tracking inside `.agent`.

## Planning Documents
- `.agent/docs` contains product, architecture, and specification documents.
- `.agent/plans` contains the active implementation plan and per-step execution documents such as `STEP1.md`, `STEP2.md`, and so on.
- `.agent/done` contains completed step files after implementation for that step has finished.

## Required Workflow
- Before implementation, review the relevant documents in `.agent/docs` and the active step files in `.agent/plans`.
- Execute work according to the current step file.
- When a step is completed, move that step file from `.agent/plans` to `.agent/done`.
- After moving the completed step file, update `.agent/plans/README.md` with a brief note describing what was completed.
- Keep the update in `.agent/plans/README.md` concise and status-oriented.

## Status Update Rule
- Each completed step must leave a short completion note in `.agent/plans/README.md`.
- The note should state which step finished and the main outcome, without long changelog-style detail.
- Do not leave completed step files in `.agent/plans`; move them to `.agent/done` once done.

## Repository Convention
- Treat `.agent/docs/product-design.md` as the product and architecture source of truth unless a newer document replaces it.
- Treat `.agent/plans/README.md` as the active execution index.
- Treat `.agent/done` as the archive of completed execution steps.

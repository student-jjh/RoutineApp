# Strength load guidance: design decision

## Shipped foundation

Expanded the seven muscle-group catalogs without renaming existing entries, preserving history matching. Search ignores spaces and includes custom exercises. The entry screen shows exact prior sets for the same exercise and muscle group in the same routine, strictly before the selected date; edited records and future sessions are excluded. This is historical reference, not an AI prediction or a validated safe starting load. Loading those sets remains an explicit action.

## Why not deploy an AI weight predictor yet?

Current records do not distinguish warm-up/work sets, assistance from added resistance, machine identity, dumbbell per-hand versus total load, or effort. A larger kg value does not necessarily mean better performance (especially on assisted machines). There is no labeled training dataset or prospective validation. An LLM-generated kg number would not address these gaps. No model or external AI service is connected in this release.

## Next implementation stages

1. Introduce stable exercise IDs and equipment/load semantics with aliases for legacy names. Add duration-based sets for timed exercises; the existing plank entry still uses the old repetition-only editor. Preserve old records as unknown when units cannot be inferred.
2. Collect optional RIR (repetitions in reserve), work/warm-up set type, target repetition range, equipment increments and a session discomfort flag. Update Room migrations, export/import validation and old-backup compatibility together. Missing effort must stay unknown.
3. Implement an explainable on-device rule engine: only compare matching equipment/units; first propose maintaining a recent successfully completed working load. Consider the smallest available increase only after repeated target-range success with sufficient reserve. Suppress numerical advice with sparse/stale history, discomfort, unknown units or assisted/bodyweight modes. Exact thresholds and staleness windows require validation; they are not established by the sources below.
4. Test retrospective replay without future-data leakage and then an opt-in prospective pilot. Measure suggestion acceptance, actual completion, error by equipment and suppression frequency. A learned model is justified only if it improves on the transparent baseline. LLMs could explain bounded results, but should not independently select loads. External processing would require an explicit product/privacy decision.

## Evidence and limits

- ACSM 2026 position-stand summary: individualize training to goals and ability; no universal kilogram prescription. https://acsm.org/resistance-training-guidelines-update-2026/
- RIR load-prescription reliability study (2022): relevant to effort-aware guidance, but not validation of this app or all exercises/users. https://pubmed.ncbi.nlm.nih.gov/36135029/
- Review of regulation/monitoring methods (2021): performance and effort feedback can inform individualization. https://pubmed.ncbi.nlm.nih.gov/33312273/

These sources inform the design, not a claim that a personalized AI model has been trained or clinically validated.

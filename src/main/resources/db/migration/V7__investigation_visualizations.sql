-- M12.3: canonical, validated and evidence-bound visualization snapshots.
ALTER TABLE investigation
    ADD COLUMN visualizations JSONB NOT NULL DEFAULT '[]'::jsonb;

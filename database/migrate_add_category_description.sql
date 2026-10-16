-- =====================================================
-- MIGRATION: Add description and image_url columns to categories table
-- Run this script if your categories table was created before these columns were added
-- =====================================================

USE aura_cafe_erp;

-- Add description column if not exists
ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS description TEXT AFTER icon;

-- Add image_url column if not exists
ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(500) AFTER description;

-- Update existing rows with default descriptions if null
UPDATE categories SET description = '' WHERE description IS NULL;
UPDATE categories SET image_url = '' WHERE image_url IS NULL;

DO $$
BEGIN

  -- 1. create enum type for link precedence
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'link_precedence') THEN
    CREATE TYPE link_precedence AS ENUM (
      'PRIMARY',
      'SECONDARY'
    );
  END IF;

  -- 2. create contact table
  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.tables
     WHERE table_name = 'contact'
  ) THEN
    CREATE TABLE contact (
      id               SERIAL PRIMARY KEY,
      phone_number     VARCHAR(20),
      email            VARCHAR(255),
      linked_id        INT,
      link_precedence  link_precedence NOT NULL,
      created_at       TIMESTAMP NOT NULL DEFAULT now(),
      updated_at       TIMESTAMP NOT NULL DEFAULT now(),
      deleted_at       TIMESTAMP,
      CONSTRAINT fk_contact_linked
        FOREIGN KEY (linked_id)
        REFERENCES contact (id)
    );
  END IF;

  -- 3. index on email
  IF NOT EXISTS (
    SELECT 1
      FROM pg_indexes
     WHERE tablename = 'contact'
       AND indexname = 'idx_contact_email'
  ) THEN
    CREATE INDEX idx_contact_email
      ON contact (email);
  END IF;

  -- 4. index on phone_number
  IF NOT EXISTS (
    SELECT 1
      FROM pg_indexes
     WHERE tablename = 'contact'
       AND indexname = 'idx_contact_phone_number'
  ) THEN
    CREATE INDEX idx_contact_phone_number
      ON contact (phone_number);
  END IF;

END;
$$;

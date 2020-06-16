CREATE TABLE [step_type] (
  [id] integer 				PRIMARY KEY,
  [name] text 				NOT NULL		UNIQUE	
);

CREATE TABLE [units] (
  [id] integer 				PRIMARY KEY,
  [name] text 				NOT NULL		UNIQUE
);

CREATE TABLE [steps] (
  [id] integer 				PRIMARY KEY,
  [step_type_id] integer,
  [title] text				NOT NULL,
  [subtitle] text,

  FOREIGN KEY ([step_type_id]) REFERENCES [step_type] ([id])
);

CREATE TABLE [recipes] (
  [id] integer 				PRIMARY KEY,
  [unit_id] integer,
  [name] text				NOT NULL,
  [description] text,
  [units] integer			NOT NULL,
  [weight] double			NOT NULL,
  [density] double,

  FOREIGN KEY ([unit_id]) REFERENCES [units] ([id]),

  -- combination of these two fields must be unique
  unique(name, description)
);

CREATE TABLE [ingredients] (
  [id] integer 				PRIMARY KEY,
  [name] text				NOT NULL,
  [description] text,
  [unit_id] integer,
  [weight] real				NOT NULL,
  [density] real			NOT NULL,

  FOREIGN KEY ([unit_id]) REFERENCES [units] ([id]),
  -- combination of these two fields must be unique
  unique(name, description)
);

CREATE TABLE [recipe_steps] (
  [id] integer 				PRIMARY KEY,
  [ingredient_id] integer,
  [recipe_id] integer,
  [step_id] integer,
  [seconds] integer,
  [weight] real,

  FOREIGN KEY ([ingredient_id]) REFERENCES [ingredients] ([id])
  FOREIGN KEY ([recipe_id]) REFERENCES [recipes] ([id])
  FOREIGN KEY ([step_id]) REFERENCES [steps] ([id])
);

-- this table is required by android
CREATE TABLE [android_metadata] (
  [locale] text DEFAULT [en_US]
);

-- table must have this populated
INSERT INTO [android_metadata] VALUES ('en_US')

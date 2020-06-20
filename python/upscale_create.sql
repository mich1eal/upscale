CREATE TABLE [step_types] (
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

	FOREIGN KEY ([step_type_id]) REFERENCES [step_types] ([id])
);

CREATE TABLE [recipes] (
	[id] integer 				PRIMARY KEY,
	[unit_id] integer,
	[name] text         		NOT NULL,
	[description] text,
	[unit_weight] integer		NOT NULL,
	[unit_count] integer		NOT NULL,
	[weight] double				NOT NULL,
	[density] double,

	FOREIGN KEY ([unit_id]) REFERENCES [units] ([id]),

	-- combination of these two fields must be unique
	unique(name, description)
);

CREATE TABLE [ingredients] (
	[id] integer 				PRIMARY KEY,
	[name] text					NOT NULL,
	[description] text,
	[unit_id] integer,
	[unit_weight] real,
	[density] real,

	FOREIGN KEY ([unit_id]) REFERENCES [units] ([id]),

	-- combination of these two fields must be unique
	unique(name, description)

	-- unit_weight and unit_id must both be either null or not null
	CHECK ((unit_weight IS NULL AND unit_id IS NULL) OR (unit_weight IS NOT NULL and unit_id IS NOT NULL))

	-- either weight or density must be filled
	CHECK (density IS NOT NULL OR unit_weight IS NOT NULL)
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

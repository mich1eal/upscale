CREATE TABLE [recipes] (
	[id] integer 				PRIMARY KEY,
	[name] text         		NOT NULL,
	[description] text,
	[servings] integer			NOT NULL,
	[unit_count] integer		NOT NULL,
	[weight] double				NOT NULL,

	unique(name)
);

CREATE TABLE [recipe_steps] (
	[id] integer 				PRIMARY KEY,
	[recipe_id] integer			NOT NULL,
	[step_order] integer		NOT NULL,
	[seconds] integer,
	[weight] real,
	[type] text,
	[ingredient] text,

	FOREIGN KEY ([recipe_id]) REFERENCES [recipes] ([id])
);

-- this table is required by android
CREATE TABLE [android_metadata] (
	[locale] text DEFAULT [en_US]
);

-- table must have this populated
INSERT INTO [android_metadata] VALUES ('en_US')

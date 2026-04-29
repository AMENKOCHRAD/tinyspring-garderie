INSERT INTO alternative_dishes
(meal_type, original_allergen, name, description, allergens, priority)
VALUES
    ('ENTREE', 'gluten', 'Soupe de légumes doux', 'Soupe légère aux légumes cuits, sans gluten.', '', 1),
    ('ENTREE', 'gluten', 'Salade de carottes cuites', 'Carottes douces assaisonnées simplement, sans gluten.', '', 2),

    ('PLAT_PRINCIPAL', 'gluten', 'Riz aux légumes doux', 'Riz tendre accompagné de légumes cuits, adapté sans gluten.', '', 1),
    ('PLAT_PRINCIPAL', 'gluten', 'Pommes de terre vapeur aux légumes', 'Pommes de terre douces avec légumes cuits, sans gluten.', '', 2),
    ('PLAT_PRINCIPAL', 'gluten', 'Poulet aux légumes et riz', 'Poulet tendre avec riz et légumes doux, sans gluten.', '', 3),

    ('DESSERT', 'gluten', 'Compote de pommes', 'Dessert doux à base de pommes cuites, sans gluten.', '', 1),
    ('GOUTER', 'gluten', 'Fruits frais coupés', 'Goûter naturel avec fruits de saison, sans gluten.', '', 1),

    ('ENTREE', 'lait', 'Soupe de légumes', 'Soupe simple sans lait ni crème.', '', 1),
    ('PLAT_PRINCIPAL', 'lait', 'Riz au poulet et légumes', 'Plat doux sans produits laitiers.', '', 1),
    ('DESSERT', 'lait', 'Compote de fruits', 'Dessert fruité sans produits laitiers.', '', 1),
    ('GOUTER', 'lait', 'Fruit de saison', 'Goûter simple sans lait.', '', 1),

    ('ENTREE', 'oeufs', 'Salade de carottes', 'Entrée douce sans œufs.', '', 1),
    ('PLAT_PRINCIPAL', 'oeufs', 'Poulet aux légumes', 'Poulet tendre avec légumes cuits, sans œufs.', '', 1),
    ('DESSERT', 'oeufs', 'Fruit frais', 'Dessert naturel sans œufs.', '', 1),
    ('GOUTER', 'oeufs', 'Compote maison', 'Goûter doux sans œufs.', '', 1),

    ('ENTREE', 'poisson', 'Soupe de légumes', 'Entrée chaude sans poisson.', '', 1),
    ('PLAT_PRINCIPAL', 'poisson', 'Poulet aux légumes', 'Plat doux au poulet, sans poisson.', '', 1),
    ('DESSERT', 'poisson', 'Compote de pommes', 'Dessert doux sans poisson.', '', 1),
    ('GOUTER', 'poisson', 'Fruit de saison', 'Goûter simple sans poisson.', '', 1),

    ('ENTREE', 'arachides', 'Soupe de légumes', 'Entrée simple sans arachides.', '', 1),
    ('PLAT_PRINCIPAL', 'arachides', 'Riz aux légumes', 'Plat doux sans arachides.', '', 1),
    ('DESSERT', 'arachides', 'Compote de pommes', 'Dessert sans arachides.', '', 1),
    ('GOUTER', 'arachides', 'Fruits frais coupés', 'Goûter sans arachides.', '', 1),

    ('ENTREE', 'noix', 'Soupe de légumes', 'Entrée simple sans noix.', '', 1),
    ('PLAT_PRINCIPAL', 'noix', 'Riz aux légumes', 'Plat doux sans noix.', '', 1),
    ('DESSERT', 'noix', 'Compote de fruits', 'Dessert sans noix.', '', 1),
    ('GOUTER', 'noix', 'Fruit de saison', 'Goûter sans noix.', '', 1),

    ('ENTREE', 'soja', 'Soupe de légumes', 'Entrée simple sans soja.', '', 1),
    ('PLAT_PRINCIPAL', 'soja', 'Poulet aux légumes', 'Plat doux sans soja.', '', 1),
    ('DESSERT', 'soja', 'Compote de pommes', 'Dessert sans soja.', '', 1),
    ('GOUTER', 'soja', 'Fruit de saison', 'Goûter sans soja.', '', 1),

    ('ENTREE', 'céleri', 'Salade de carottes', 'Entrée douce sans céleri.', '', 1),
    ('PLAT_PRINCIPAL', 'céleri', 'Riz aux légumes doux', 'Plat simple sans céleri.', '', 1),
    ('DESSERT', 'céleri', 'Compote de pommes', 'Dessert sans céleri.', '', 1),
    ('GOUTER', 'céleri', 'Fruit de saison', 'Goûter sans céleri.', '', 1),

    ('ENTREE', 'moutarde', 'Soupe de légumes doux', 'Entrée sans moutarde.', '', 1),
    ('PLAT_PRINCIPAL', 'moutarde', 'Riz au poulet et légumes', 'Plat sans moutarde.', '', 1),
    ('DESSERT', 'moutarde', 'Compote de fruits', 'Dessert sans moutarde.', '', 1),
    ('GOUTER', 'moutarde', 'Fruits frais coupés', 'Goûter sans moutarde.', '', 1),

    ('ENTREE', 'sésame', 'Soupe de légumes', 'Entrée sans sésame.', '', 1),
    ('PLAT_PRINCIPAL', 'sésame', 'Riz aux légumes', 'Plat sans sésame.', '', 1),
    ('DESSERT', 'sésame', 'Compote de pommes', 'Dessert sans sésame.', '', 1),
    ('GOUTER', 'sésame', 'Fruit de saison', 'Goûter sans sésame.', '', 1),

    ('ENTREE', 'sulfites', 'Soupe de légumes maison', 'Entrée simple sans sulfites.', '', 1),
    ('PLAT_PRINCIPAL', 'sulfites', 'Poulet aux légumes', 'Plat sans sulfites.', '', 1),
    ('DESSERT', 'sulfites', 'Fruit frais', 'Dessert sans sulfites.', '', 1),
    ('GOUTER', 'sulfites', 'Fruits frais coupés', 'Goûter sans sulfites.', '', 1);
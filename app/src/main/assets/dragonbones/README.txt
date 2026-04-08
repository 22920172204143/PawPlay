DragonBones Asset Directory
===========================

Each prey type has its own folder. Export from DragonBones Pro editor
and place the 3 files into the corresponding folder:

  dragonbones/
    bug/
      bug_ske.json    <-- skeleton export
      bug_tex.json    <-- texture atlas descriptor
      bug_tex.png     <-- texture atlas image
    fish/
      fish_ske.json
      fish_tex.json
      fish_tex.png
    ...

Naming convention:
  {name}_ske.json   -- skeleton data
  {name}_tex.json   -- texture atlas data
  {name}_tex.png    -- texture atlas image

Folder-to-ToyID mapping:
  bug        -> Toy ID 1  (Ladybug / Firefly)
  fish       -> Toy ID 2  (Fish)
  cockroach  -> Toy ID 3  (Cockroach)
  butterfly  -> Toy ID 4  (Butterfly)
  mouse      -> Toy ID 5  (Mouse)
  spider     -> Toy ID 6  (Spider)
  bee        -> Toy ID 7  (Bee)
  feather    -> Toy ID 8  (Feather)
  bird       -> Toy ID 9  (Bird)
  yarn       -> Toy ID 10 (Yarn Ball)

If no DragonBones asset is found for a toy, the code-drawn fallback
(PreyDrawer.kt) is used automatically.

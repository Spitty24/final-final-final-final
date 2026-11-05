# Freerun — Mirror's Edge–style parkour for Minecraft (Fabric 1.21.1)

Wall runs, wall climbs, ledge grabs and shimmying, mantles, vaults, slides and landing rolls,
with animated arms in first person and full-body animation in third person (visible to other
players when the server has the mod too).

> **Status:** builds cleanly on GitHub Actions against Minecraft 1.21.1. Newer features
> (most recently the 20× city, the new stores and drive-thrus, police and crime) haven't been play-tested yet, so expect
> some tuning.

## Build the .jar

**Option A — GitHub, no setup (works from a phone):**
1. Create a new GitHub repository and upload everything in this folder (keep the `.github` folder).
2. Open the **Actions** tab → the latest **build** run → download the **freerun-mod** artifact.
3. Unzip it and use `freerun-1.0.0.jar` (not the `-sources` one).

**Option B — on your PC:** install Java 21, then in this folder run `gradlew build`
(`./gradlew build` on Mac/Linux). The jar appears in `build/libs/`.

## Install

1. Install **Fabric Loader** for **Minecraft 1.21.1** with the installer from https://fabricmc.net/use/.
2. Put these in your `.minecraft/mods` folder:
   - `freerun-1.0.0.jar`
   - **Fabric API** for 1.21.1 (Modrinth or CurseForge)
3. Start the **fabric-loader-1.21.1** profile in the Minecraft launcher.

Multiplayer: the client mod works on its own, but other players only see your animations and
the roll/fall-damage protection only works if the **server** also has Freerun + Fabric API.

## Controls

Uses your normal keys — **Jump** is "up", **Sneak** is "down". Sprint first.

| Move | How |
|---|---|
| Vault | Sprint into a waist-high obstacle (or press Jump at it) |
| Wall climb | Sprint at a wall and Jump |
| Mantle | Automatic at the top of a wall climb |
| Ledge grab | Jump/fall at a ledge while holding Forward |
| Hang: climb up / drop / shimmy | Jump / Sneak / Left & Right |
| Hang: leap away | Back + Jump (turns you around) |
| Wall run | Jump alongside a wall while sprinting, hold Forward |
| Wall jump | Jump during a wall run (chain to the opposite wall) |
| Wall kick | Jump during a wall climb (turns 180°) |
| Slide | Sneak while sprinting; Jump out to keep speed |
| Landing roll | Tap Sneak while falling; absorbs ~12 blocks of fall damage |
| Toggle mod | **K** (rebind in Controls → Freerun) |
| Blackwire hack | **H** / **Shift+H** while holding your phone |
| Skill tree | **P** |

## Careers and skill trees

The first time you join a world you pick a career: **Employee**, **Supervisor**,
**Superintendent** or **Upper Management**. Press **P** any time to open your skill tree (or the
picker, if you haven't chosen). You can switch careers from the tree; your skills reset but your
level stays.

**Prestige:** once every skill in your current tree is unlocked, a **★ Get Promoted** button
appears (click it twice to confirm). You start the next career at level 1, and every skill you
already own keeps working. Finished careers show as ★ tabs in the tree (view only) and can't be
picked again. Finish all four and you're done.

**Levelling:** career XP comes from doing the job. Pressing a part +5, welding +8, cutting welds
+2, a crane lift +4 (setting it down +2), a forklift lift +4, making boudin +3, beating a hostile
mob +4 (a gangster +6), every 6 blocks mined +1, and +2 for every minute on the clock, +2 for a cup of coffee. Level N
needs 30 + 15×N XP to reach the next; the cap is level 20. Every level gives one skill point
(you start with one). A small readout in the top-left corner shows XP as you earn it.

**Trees:** thirteen skills per career in five tiers. Tiers open at levels **1, 3, 6, 9 and 12**
(so you reach the next tier much sooner than before), and each tier past the first needs a skill
from the tier before it. The capstone sits alone in tier 5.

| Career | Tier 1 (Lv 1) | Tier 2 (Lv 3) | Tier 3 (Lv 6) | Tier 4 (Lv 9) | Tier 5 (Lv 12) |
|---|---|---|---|---|---|
| Employee | Hard Worker (Mine 15% faster) · Steel-Toed Boots (+2 armor, and 25% less fall damage) · Union Card (Your paycheck is 20% bigger) | Forklift Certified (Forklifts drive 35% faster and their forks lift twice as fast for you) · Overtime (Earn double XP just for being on the clock) · Safety Glasses (Blindness and Darkness wear off instantly) | Packed Lunch (A fresh link of boudin turns up in your lunchbox every 8 minutes) · Strong Back (+2 hearts and 30% knockback resistance) · Second Wind (Below 4 hearts, you start regenerating) | Ride Share (Cars you drive have 20% more top speed and pick-up) · Coffee Run (A fresh Cup of Coffee turns up every 6 minutes) · Shop Floor Legend (+3 hearts) | Employee of the Month (Move 12% faster, mine another 15% faster, and earn 25% more XP) |
| Supervisor | Clipboard (+1 attack damage. Writing people up hits hard) · Safety First (40% less fall damage, and fall 2 blocks further before it hurts) · Open Door Policy (Shops give you 10% off) | Walking the Floor (Move 8% faster) · Write-Up (Your hits knock things much further back) · Loss Prevention (Stopping a crime pays double) | Eagle Eye (Permanent night vision. Nothing on the floor gets past you) · Sick and Tired (+2 more attack damage and 20% faster attacks) · Police Radio (You hear every crime reported anywhere in the city) | Chain of Command (+3 armor toughness) · Overtime Approved (Your paycheck is 25% bigger) · Quality Control (Stores pay you 15% more for what you sell) | Promotion Track (Earn 40% more XP, and players near you get Speed) |
| Superintendent | Site Walk (Move 10% faster) · Heavy Equipment (Overhead cranes drive 60% faster for you, and forklift forks lift twice as fast) · Hard Hat (+2 armor) | Long Reach (Reach blocks 2 further and entities 1 further) · Mining Operations (Mine 25% faster) · Site Radio (Blackwire hacks use 30% less battery) | Fire Watch (Permanent fire resistance) · Step Up (Walk straight up full blocks without jumping) · Road Crew (Cars you drive accelerate harder and turn tighter) | Utility Access (Hacking traffic lights and street lights costs no battery) · Scaffold Legs (Jump higher and fall 3 blocks further safely) · Zoning Commissioner (+$25 on every paycheck) | Shutdown Authority (Permanent Haste II and +2 hearts. The site runs on your say-so) |
| Upper Management | Expense Account (Collect 2 gold ingots every 10 minutes) · Corner Office (+2 hearts. The view is great for your health) · Company Card (Shops give you 15% off) | Synergy (+2 luck: better fishing and better loot) · Executive Presence (Permanent Hero of the Village: villagers give you discounts) · Insider Trading (Hacks that pay out cash pay 50% more) | Golden Parachute (Whenever you fall, you float gently down instead) · Quarterly Bonus (Earn 50% more XP, plus a diamond every 30 minutes) · Bodyguard Detail (The police never come after you) | Stock Options ($5 lands in your account every 2 minutes) · Private Security (Permanent Resistance) · Board Seat (Your paycheck is 40% bigger) | Hostile Takeover (+4 attack damage, +4 armor, and slow constant regeneration) |

P is also vanilla's Social Interactions key; the skill tree reads the key directly so it opens
either way. Rebind it under Controls → Freerun if you like.

## The city

Cities are now about **20 times bigger: 1,084 × 1,084 blocks**, an 18 × 18 grid of city blocks
(the old one was 244 × 244). Most 2,048-block regions of the overworld get one (and there's
almost always one within a couple of kilometres of spawn), on reasonably dry, reasonably flat land; hills are cut down, hollows and rivers filled in, and the edges sloped
off. Type **`/findcity`** to get the nearest one's location and direction, or **`/city`** (needs
cheats) to found one centred on you, at the height of the ground around you. A city that size is
built chunk by chunk as you explore (the part around you goes up within seconds). If a chunk of
a city ever comes out missing, it's filled in automatically the next time it loads.

![City layout](docs/city_layout.png)

- **Downtown:** office towers 5–10 floors tall around **City Hall** and the central park.
- **The commercial ring:** two **MegaMart Supercenters**, two **Hargrove & Finch** department
  stores, six strip malls, and plenty of **Big Bun Burgers**, **Cluck Shacks**, **Sugar Bayou
  Sweets**, Double Quicks, General Stores and Texas Roadhouses, with **parking lots** joined up by
  paved service lanes so you can drive from one lot to the next. Stores are spread out: you won't
  find two of the same restaurant or shop within about 230 blocks of each other.
- **The Hood** (south-west), with apartments, corner stores, community gardens, basketball courts
  and the odd vacant lot that's Rust Rat / Chrome King turf.
- **The welding factory** (south-east), ringed by parking.
- **Suburbs** everywhere else, with parks, corner stores and **five police precincts** spread
  through them.

### Stores

| Store | What it is | Sells |
|---|---|---|
| **MegaMart Supercenter** | Big-box store with a huge lot, checkouts, a service desk, a pharmacy | Groceries, snacks and drinks in bulk, electronics, home & garden, sporting goods, ammo, clothes |
| **Hargrove & Finch** | Two-storey department store with an Auto Center | Tools (iron and diamond), appliances, furniture, apparel and jewellery, a car; buys gold, gems and steel |
| **Big Bun Burgers** | Burger joint with a drive-thru | Big Bun, Double Big Bun, Bacon Stacker, crinkle fries, onion rings, shakes, the Bun Buddy Meal |
| **Cluck Shack** | Fried chicken with a drive-thru | Cluck Sandwich, Spicy Cluck, nuggets, hot wings, honey biscuits, red beans & rice, mac & cheese |
| **Sugar Bayou Sweets** | Bakery and sno-ball stand (also in every strip mall) | Beignets, pralines, king cake (mind the baby), bread pudding, sno-balls, cupcakes, soft serve, pecan pie, brownies |
| **Strip malls** | Six storefronts and a shared lot | Sugar Bayou Sweets, Crescent Hardware & Cellular, plus a laundromat, pawn shop, bookstore, nail salon, po'boy shop or dry cleaner |

All 24 new foods are edible and most have an effect (speed, absorption, fire resistance, brain
freeze, a food coma...). Supervisors' **Open Door Policy** and Upper Management's **Company Card**
take 10% / 15% off everywhere.

**Drive-thru:** at Big Bun Burgers and the Cluck Shack, drive into the lane down the side of the
building, stop at the **menu board** on the back and **right-click it from your car**. The crew
answers over the speaker and the menu opens; pay, then drive round to the **pickup window** and
your bag is handed through as soon as you pull up beside it.

### Streets and traffic

Two-lane roads with sidewalks, crosswalks, street lamps, hydrants and benches. Every third street
is an arterial with **traffic lights**; everywhere else has **stop signs**. Residents drive the
city's cars: they keep to their lane, stop for red lights, come to a full stop at stop signs,
brake for whatever's in front of them, and pick a way to go at each intersection.

Fixes for cars getting stuck at intersections:
- At a stop sign, drivers now **take turns**: one car holds the intersection at a time.
- Drivers **don't pull into an intersection** unless there's room on the other side.
- **Oncoming traffic** and cars waiting at the other stop lines no longer count as "in the way",
  and pedestrians on the sidewalk don't make cars brake.
- A car wedged for 8 seconds **ghosts** through other cars for a few seconds to get loose.
- **Jam detection:** every 5 seconds the city looks for three or more cars stuck at the same
  intersection and waves them all through ("Traffic control cleared a jam nearby").
- As before, a car stuck for 20 seconds with nobody watching is put back in its lane, and
  **`/cityclear`** (cheats) unjams everything within 256 blocks.

### People

The streets around you are always busy: around **34 people on the sidewalks** (14 at night),
**13 cars** on the roads (7 at night) and a couple of **police cruisers** on patrol. New people
step out a good way off, never right in front of you; anyone the city brought out is tidied away
once no player is within 150 blocks. Residents each have a name, age, job, home and bank balance,
and live a daily routine near home: work, errands at the nearby stores, then home and rent.
They run from fights and crimes. Right-click one to say hi.

### Police and crime

**Police officers** patrol in black-and-white cruisers with roof light bars, and work out of the
precincts. **Crime is rare:** for each player in a city, every 2½ to 5 minutes there's about a
one-in-three chance something happens nearby:

| Crime | What happens |
|---|---|
| Purse snatching | Someone grabs a resident's money and runs |
| Armed mugging | A suspect holds a resident up at gunpoint, then runs (and shoots back if you start something) |
| Car theft | A suspect pulls a driver out of their car and speeds off, running every light |

It's called in on the radio to everyone within 200 blocks (Supervisors with **Police Radio** hear
the whole city), and the nearest officer responds with **lights and siren**, drives straight
there, and goes after the suspect on foot. **Stop it yourself:** take the suspect down until they
put their hands up, then **right-click them to cuff them**. The city pays **$60–150** and career
XP (Supervisors' **Loss Prevention** doubles it), and the victim gets their money back. Killing a
suspect only pays a third. The phone's City app keeps count of crimes, how many were stopped,
and how many by the police.

**Wanted:** hurting residents makes you wanted (★ for a fight, ★★ for a killing, ★★★ for shooting
at an officer) and the nearest officer comes for you. At one star they arrest you (a fine); at two
or more they draw their weapons. Stay out of sight for a minute to lose them. Upper Management's
**Bodyguard Detail** means the police never come after you.

### City economy

Each city keeps its books in the world's saved data: wages paid, rent collected and what everyone
spends in the shops. Store prices follow demand: a busy day pushes them up (to +30%), a slow one
brings discounts (to −20%). The City app on your phone shows the numbers.

## Smartphone

Right-click with the Smartphone to open it. Apps:

| App | What it shows |
|---|---|
| Bank | Your balance, wallet, next paycheck, local price level |
| Map | The whole city, a district, or your street (scroll or +/- to zoom), store names, police (blue) and suspects (flashing red); outside a city, a compass to the nearest one |
| Blackwire | Hacking: battery, settings, what you can hack |
| Jobs | Your career, paycheck and the next break |
| City | People about, jobs, savings, cars, today's wages/sales/rent, price index, crimes reported and stopped |
| Messages | Texts, and a log of your hacks |

**Blackwire** (hacking): hold the phone (or turn on "always" in the app) and whatever you look at
within 48 blocks gets profiled: a resident's name, age, job, income, bank balance and something
they'd rather you didn't know. Press **H** to hack, **Shift+H** for the second option. Hacks use
phone battery, which recharges by itself.

| Target | H | Shift+H |
|---|---|---|
| Resident | Transfer $10–60 from their account | Flood their phone; they stop and stare at it |
| Car with a driver | Kill the engine | Floor it through every light |
| Parked car | Set off the alarm | — |
| Traffic light | Every direction green (crashes) | Every direction red (gridlock) |
| Street lamp | Black out the block for 30 s | — |
| ATM | Dispense $10–40 (once per ATM per 10 min) | — |
| Shop clerk | Skim the register (once per 5 min) | — |
| Gangster | Overload their phone (zap) | Leak their location to the other crew |
| Gas pump | Overload it (boom) | — |
| Vending machine / TV | Free snack / change the channel | — |

## Welding factory

The factory is part of every city, or build one on its own with **`/factory`** (needs cheats;
the parking lot starts just ahead of you and the building is past it). Inside:

- a press department with five working press brakes (big 3×3 machines with side housings,
  hydraulic cylinders, a ram, back gauge, light curtains and a foot pedal) and their operators, who drop each part
  they form onto a **conveyor** that carries it to a bin barrel by the aisle
- a welding department with four curtained booths and welders at work
- a working **overhead crane** spanning the whole floor
- two **forklifts** on their rounds: one driven by Boudreaux (pure bayou gibberish), the other by
  **Johnny Sim**, a balding Cajun driver you can actually understand; plus a parked forklift by
  the door for you, with two sheets in front of it to practise on
- three **co-op interns** (Tyler, Brayden and Kaitlyn) wandering around asking questions and
  following Ethan because he seems like he knows things
- **Ethan James Patrick**, enderman-tall and slow, wandering the floor asking questions
- a **break room** off the floor: counters you can store things in, a working coffee maker and
  microwave, tables and chairs, a small coffee patch, and workers on break
- a **processing plant** next door with Sausage Stuffers, a sausage maker at work, a grill and
  smoker, cold storage and recipe signs
- steel stock racks, a public weld table, and a gear chest at the back holding a Gravity Gun,
  Welding Torch, Crane Pendant, conveyor belts, a Forklift and steel
- the **Double Quick** gas station east of the parking lot and a **General Store** beside the
  factory (see Economy below)
- a **parking lot** out front: drivable cars (and one burnt-out wreck), light poles, and two
  made-up masked crews, the Rust Rats (orange) and the Chrome Kings (purple). They trash-talk
  each other and every 2–5 minutes start a shootout. They leave you alone unless you attack one,
  then that whole crew comes for you — but stray shots don't care who you are. Lost members are
  replaced over time. They don't exist on Peaceful.

Factories generated before an update keep their old layout; use `/factory` or find a new one.

**Steel parts** (sheet, plate, bracket, channel) are free physics objects: they fall, bounce,
slide, stack, and you can stand on them. They stay upright and turn in 90° steps (no tumbling).

| Action | How |
|---|---|
| Place a part | Right-click a block with a part item |
| Stack a part on an assembly | Right-click the assembly with a part item |
| Rotate a part / assembly | Right-click it with an empty hand |
| Pick it back up | Sneak + right-click with an empty hand |
| Shove it | Punch it |
| Press brake: form steel | Right-click it holding a sheet or plate |
| Press brake: switch CUT / BEND | Right-click it with an empty hand |
| Get a welding torch | Right-click a Welder machine with an empty hand |
| Weld | Torch: click a part, then a part touching it (needs a Welder within 12 blocks) |
| Cut welds apart | Torch: sneak + click an assembly |
| Gravity gun: grab / drop | Right-click |
| Gravity gun: launch | Sneak + right-click |
| Crane: drive the hook | Hold right-click with the Crane Pendant; the hook goes where you aim (red laser dot marks the spot) |
| Crane: pick up / set down | Sneak + right-click with the pendant |
| Forklift: get on / off | Right-click an empty forklift / Sneak |
| Forklift: drive | W/S drive, A/D steer (it beeps in reverse) |
| Forklift: forks up / down | **R** / **V** (rebind in Controls → Freerun) |
| Forklift: pick up as an item | Sneak + punch an empty forklift |
| Conveyor | Place it and it runs away from you; a redstone signal stops it |
| Make sausage | Right-click a Sausage Stuffer with a filler crop to load it (optional), then with meat (makes 2). Empty hand takes the filler back out |
| Coffee | Put Coffee Grounds in a Coffee Maker (brews 4 cups), then right-click it with a Coffee Mug |
| Microwave | Right-click with food: raw food cooks, cooked food just gets warm. Don't put metal in it |
| Counter | Right-click to open its 27-slot storage |

Press brake results: **CUT** turns a sheet into 2 plates; **BEND** turns a sheet into a bracket
and a plate into a channel. The gravity gun also lifts dropped items, most mobs, minecarts,
boats, falling blocks and lit TNT. Boudin fills 4 drumsticks of hunger and gives a short burst of Speed; the
forklift drivers love it if you hand them some.

### Sausages

| Meat | Filler | Sausage | Effect |
|---|---|---|---|
| Pork | none or wheat | Boudin | Speed |
| Pork | beetroot | Chorizo | Fire Resistance |
| Pork | sweet berries | Breakfast Links | Regeneration |
| Beef | none | Beef Frank | — |
| Beef | potato | Kielbasa | Resistance |
| Beef | wheat | Bratwurst | Absorption |
| Chicken | apple | Chicken Apple Sausage | Regeneration |
| Mutton | beetroot | Merguez | Strength |
| Rabbit | carrot | Hunter's Sausage | Jump Boost |
| Cod or salmon | dried kelp | Seafood Sausage | Water Breathing |
| Rotten flesh | the fake "Stone" | Chud Sausage | **Chud-Maxing** (5 min) |

**Chud-Maxing** makes you visibly overweight (other players see it too), 15% slower, harder to
knock back and gives +2 hearts. Fake Stones that are alive have extra things to say about it.

### Coffee

Craft Coffee Beans from cocoa beans + wheat seeds, plant them on farmland, and harvest Coffee
Cherries when they turn red. Roast the cherries in a furnace, smoker or campfire, then craft the
roasted beans into Coffee Grounds. A Cup of Coffee gives Haste and Speed and returns the mug.

Recipes: steel sheet (3 iron ingots in a row → 2), Welder (iron, redstone, copper), Press Brake
(iron, piston, smooth stone), Welding Torch (copper ingot + iron ingot + flint and steel),
Gravity Gun (iron, ender pearl, redstone, diamond), Conveyor ×8 (black wool, iron, redstone),
Crane Pendant (yellow dye + iron ingot + redstone + string), Forklift (iron ingot, yellow dye,
iron block, coal blocks), Sausage Stuffer (iron ingots around a hopper), Coffee Mug ×2 (bricks
in a U), Coffee Maker (iron, glass, redstone), Microwave (iron, glass pane, redstone), Counter ×2
(smooth stone slabs over planks and a chest). Cranes only come with
factories.

## Economy, stores and break time

Money is real: **$1, $5, $20 and $100 bills**. A **Wallet** holds your cash so it doesn't fill your
inventory (use it to put loose bills in, sneak-use to take out $20); the first time you use an
**ATM** it gives you one free. Paying always takes from your wallet first, then your bills, and
gives you change. Your cash shows in the bottom-left corner.

**Earning:** every break you get a paycheck by direct deposit ($10, or $15 + $3 per career level
+ $10 per career you've mastered). You can also sell things at the stores (steel parts,
sausages, crops, ores, empty cans), win on scratch-offs, or take it off gangsters.

**Break time:** every 10 minutes the buzzer goes for a 2-minute break. The factory workers and the
co-ops walk out across the parking lot to the Double Quick, order something at the counter, eat
it, and walk back to their stations when the break ends.

**The Double Quick** (gas station): four working gas pumps under a lit canopy, a car filling up, a
checkout with the clerk, a roller grill, slushie machine, coffee bar with coffee makers and a
microwave, shelves, a wall of drink coolers, an ATM, a TV and vending machines out front. Talk to
the clerk to open the shop. **The General Store**: an old country store with a porch, hardware,
groceries, phones, ammo, store fixtures and a used car out back ($400). Talk to the shopkeeper.
Standalone Double Quicks with a General Store also generate in the countryside, or build one with
**`/doublequick`**.

In a shop screen: click an item to buy one, shift-click to buy five; the Sell tab shows what the
store pays.

**Food and drink** (all edible, most with an effect): potato chips, cheese puffs, candy bars,
gummy worms, beef jerky, sunflower seeds, Cajun boiled peanuts, pork cracklins, honey buns,
donuts, frozen burritos (microwave them into hot burritos), instant ramen, roller-grill hot dogs,
corn dogs, gas station pizza, chicken tenders, nachos, sausage biscuits, ice cream sandwiches,
pickles in a pouch, Fizz Cola, lemon-lime soda, root beer, Voltage energy drinks, sports drinks,
bottled water, sweet tea, chocolate milk, blue raspberry and cherry slushies, iced coffee. Cans
leave an Empty Can (sell 4 for $1, or smelt for iron).

**Modern conveniences:**

| Item | What it does |
|---|---|
| Smartphone | Shows the time, your balance, the next break, GPS and a notification |
| Flashlight | Use to switch on; night vision while you hold it |
| Lucky 7s Scratch-Off | Use to scratch: $2 to $1,000 prizes |
| Lighter | Lights fires like flint and steel |
| Bandages / Pain Reliever | Heal 3 hearts / clear harmful effects |
| Sunglasses | Wear them: immune to Blindness and Darkness |
| Umbrella | Hold it while falling to float down |
| Air Horn | Very loud; startles every mob nearby away from you |
| Duct Tape | Repairs a quarter of the damaged item in your other hand |
| Gas Can | Adds 40% fuel to a car; refill an empty one at a pump ($6) |

| Fixture | How it works |
|---|---|
| ATM | Click to deposit your bills, sneak-click to withdraw $20 |
| Vending Machine | $2 for a random snack or drink. If it gets stuck, punch the machine |
| Slushie Machine | $2. Click the left side for blue raspberry, the right for cherry |
| Roller Grill | Put raw pork, beef or chicken on it; 2 hot dogs come off |
| Gas Pump | Fills the nearest car within 6 blocks ($1 per 10%) |
| TV | Click to turn on or off |
| Drink Cooler / Store Shelf | Storage, like a counter |

## Cars

Every parked car in the factory lot is drivable, and you can buy one at the General Store or craft
one. Right-click to get in (four seats; the first one in drives). **W/S** gas and brake (hold S to
reverse), **A/D** steer, **Space** handbrake (the back slides out), **R** horn, sneak to get out.
The HUD shows your speed and fuel.

The physics: it accelerates along an engine curve to about 70 km/h with rolling resistance and
drag, steering eases in and recentres, the turn rate follows a bicycle model (tight at low speed,
wide at high speed, no turning standing still), and the tyres grip unless you pull the handbrake.
Cars climb curbs and single blocks, crash into walls (hard crashes hurt), and run over anything
in the way. They burn fuel; refill at a gas pump or with a gas can. Sneak-punch an empty car to
pick it up.

## Guns

Guns now fire real bullets with glowing tracers, a muzzle flash, smoke, ejected casings, impact
dust and sparks, recoil, and their own gunshot sounds. Each has a magazine that reloads from the
ammo in your inventory (sneak + use to reload early):

| Gun | Magazine | Notes |
|---|---|---|
| Pistol | 12 | Semi-auto |
| Rifle | 30 | Full auto: hold use |
| Shotgun | 6 shells | 8 pellets per shot |

Soldiers and gangsters use the same bullets. Bullets and shells are sold at the General Store.

## Ethan James Patrick

Ethan wanders the factory asking questions. One time in ten he instead says something
genuinely brilliant, then goes back to normal. Every few minutes he leaps up onto an overhead
crane and hangs from the girder, riding it around, before dropping back down (he takes no fall
damage). Hurt him and he pulls out a **plasma cannon** and fires it at you and any workers
nearby for about 15 seconds, then calms down and apologises. If you kill him while he's angry,
he drops the cannon. It's also in the creative Combat tab.

**He is secretly the leader of the Rust Rats.** The Rats never shoot him, the whole crew comes
running if anyone hurts him, and every so often he forgets the walkie-talkie is on.

**Every 3–5 minutes Supervisor Slater and Safety Nick come down to the floor** (only when a player
is within about 48 blocks). Ethan gets up on the crane, they yell at him to get the hell down before
they fire him, he lets go and lands right on Nick, and Nick smells something: **Big Boudin**, the
sausage plant's legend, a seven-foot walking link in a hard hat. He swats people with a giant
spatula for a few minutes, then waddles back to the walk-in cooler. Beat him for a pile of boudin.

**Johnny James Patrick:** hand Ethan a boudin, then hand Johnny Sim one. Ethan goes looking for
Johnny, and when they meet up there's a baby: Johnny James Patrick, Ethan's tall frame with
Johnny's bald head and half of each of their outfits. He asks Ethan's questions in Johnny's voice
and grows up over 20 minutes. They can do it again after 5 minutes.

## Robberies

Once a minute there's a 1 in 20 chance that a couple of gang members (usually Rust Rats, on the
boss's orders) walk over and hold up the Double Quick. They take up to $25 from every player in the
store plus the register, and about half the time they also shoot up the storefront. Take a
robber down and you get the cash back.

## The fake "Stone"

The pointless ore that looks exactly like stone now has a 1 in 10 chance, when you mine it, to
come alive instead of dropping. It follows you everywhere (teleporting to catch up) and calls you
a chud in chat. It's harmless; kill it and you get the stone back.

## Settings

`config/freerun.json` is created on first launch. Every move can be switched off, plus:
`wallRunTicks`, `wallClimbTicks`, `momentumBonus`, `rollFallReduction`, `cameraTilt`
(negative flips the lean), `cameraRollFlip`, `firstPersonArms`, `sprintArmPump`.

## Tuning the animations

- First-person arm poses: `FirstPersonArms.pose()` — offsets/angles per move.
- Third-person limbs: `ThirdPersonAnimator.limbPose()`; body lean/roll: `bodyParams()`.
- Movement feel (speeds, timings): `ParkourController`.

## How it works

- `ParkourController` runs at the start of each client tick, detects walls/ledges with collision
  boxes and overrides the player's velocity while a move is active.
- The current move is sent to the server, which relays it to nearby players and uses it to
  cancel/soften fall damage (`PlayerMixin`).
- Mixins: `ItemInHandRenderer` (first-person arms), `PlayerModel` (limb poses),
  `PlayerRenderer` (whole-body rotation), `GameRenderer` (camera tilt and roll).
- Factory code lives in `dev.freerun.factory` (parts, machines, torch, gravity gun, crane,
  conveyor, forklift and driver), `dev.freerun.food` (sausages, coffee, microwave, counters,
  Chud-Maxing) and `dev.freerun.world.FactoryPiece` /
  `ParkingLotPiece` / `BreakRoomPiece` / `ProcessingPiece` (the building, lot and annexes). Steel parts are simulated on the server and synced to
  clients each tick. Forklifts are driven client-side like horses; the fork keys are sent to the
  server, which runs the forks and load.
- Gangs: `dev.freerun.gang` — `GangTurfEntity` is an invisible marker in the lot that keeps the
  crews topped up and schedules shootouts and robberies.
- Economy: `dev.freerun.economy` (money, shops and their price lists in `EconomyRegistry`, break
  time, the break-time walk, store fixtures, snacks and amenities). Cars: `dev.freerun.vehicle`.
  Guns: `dev.freerun.item.gun`. The gunshots, engine, horn and other new sounds are synthesized.
- Careers: `dev.freerun.career` — the skill definitions are in `Skills`; progress is stored as a
  Fabric data attachment on the player (kept through death) and synced to the client for the
  skill tree screen and HUD.
- The Roadhouse office scene runs on a 45-second loop (`OfficeSkit`); line timings are the
  `LINE_*` constants there.

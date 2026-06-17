# Century Raffle Helper

A lightweight helper mod for the Hypixel SkyBlock Century Raffle, developed to keep your raffle progress easy to track.

Century Raffle Helper can be configured with `/crh`, and HUD elements can be moved and scaled with `/crh hud`.

## Features

- Raffle Timers: Shows Speed Raffle, Daily Raffle, and The Big One timers.
- Raffle Milestone Tracker: Tracks gained raffle tickets and shows progress toward the next milestone.
- Cake Eater HUD: Tracks 25 Century Cake slices per 2-hour Speed Raffle cycle with a synced reset timer.
- Daily Task HUD: Shows incomplete Easy, Medium, and Hard raffle tasks.
- Task Descriptions: Shows task descriptions by default instead of only task names.
- Compact Task Display: Keeps task lists readable with numbered entries and overflow indicators.
- Cake Team Glow: Highlights players on the matching cake team while holding the correct Slice Of Century Cake.
- Dev Debug Tools: Includes cake glow debug output and a highlight-all cake teams toggle for testing.
- Moveable HUD: Drag, scale, reset, and position each HUD element in the HUD editor.
- Config Menu: Toggle HUD elements, timers, task tiers, reminders, glow options, and debug tools.
- Persistent Config: Saves HUD positions, scales, and settings to the mod config.
- Hypixel Only Mode: Optionally hides the HUD when you are not connected to Hypixel.

## HUD Editor

Open the HUD editor with:

`/crh hud`

From there, you can drag HUD elements around your screen, scroll to resize them, use arrow keys for small adjustments, and reset elements when needed.

## Commands

- `/crh` - Opens the config menu.
- `/crh config` - Opens the config menu.
- `/crh hud` - Opens the HUD editor.
- `/crh gui` - Opens the HUD editor.
- `/crh edit` - Opens the HUD editor.
- `/crh reset` - Resets today's tracked raffle state.
- `/crh cakedebug` - Dumps cake glow detection data to `latest.log`.

## Note

For the most accurate information, open the Hypixel SkyBlock raffle menu at least once after joining so the mod can read your current raffle data.

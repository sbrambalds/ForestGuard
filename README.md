### ForestGuard — A Multi-Agent System for Forest Monitoring and Fire Fighting

## Description
This project proposes a simulation of a forest monitoring and fire fighting system. The agents in this simulation are represented by scout drones, firefighter drones, and a forest ranger station.

# Scout Drone

Scout drones are responsible for exploring the forest and mapping the environment. Their main task is detecting active fires and reporting them directly to the ranger station, which is always reachable thanks to its long range antenna. Scouts must continuously monitor their battery level and return to the station to recharge when it runs low.

# Firefighter Drone: 
Firefighter drones are dispatched by the station to locations where fires have been reported. Their main task is to extinguish active fires using their onboard water tank. When the tank runs empty, they must decide where to refill: if their battery level is sufficient, they head to the nearest water source (river or lake); otherwise, they return to the ranger station to both recharge and refill. Fire suppression is always their top priority, and resource management is key to their effectiveness.

# Forest Ranger Station: 
The station is a stationary agent placed at a fixed location in the grid. It acts as the central hub of the system: it recharges drone batteries, refills the firefighter water tanks, and aggregates map information reported by drones. Thanks to its long range antenna it can communicate with any drone in the forest at any time, while drones can only communicate with each other when within short antenna range. It dispatches firefighter drones to reported fire locations and can redirect scouts to unexplored areas based on the global map it maintains.

The environment is represented by a randomly generated 2D grid forest, which contains trees, clearings, obstacles, fire cells, and water sources such as rivers or lakes. Fire spreads dynamically to adjacent cells every few turns, adding urgency to the system. Drones can communicate with each other only when within short antenna range, while the station can reach any drone at any time.

# Technical Aspects
The technologies that will be implemented are:

- Jason for the programming of the agents and their behaviours
- Java for the environment, the grid simulation and the GUI

# Deployment

## Prerequisites
- `git`
- JDK 21

All the other dependencies (including the Jason interpreter) are downloaded automatically by Gradle through the included wrapper: no global Gradle installation is needed.

## Run
```bash
git clone <repository-url>
cd ForestGuard
./gradlew runForestguardMas
```

On Windows use `gradlew.bat runForestguardMas`.

The task launches the centralised Jason infrastructure on `forestguard.mas2j`, opening both the Jason MAS console and the simulation window. The simulation can be paused and its speed changed from the controls at the top of the window.

## Configuration
- Simulation parameters (grid size, number of trees and lakes, fire and spreading delays, battery factor, ...) are defined in `src/main/java/model/Config.java`.
- The agents are declared in `forestguard.mas2j`. Their names must match the ones in `Config.SCOUT_STATIONS` and `Config.FIREFIGHTER_STATIONS`, which assign each drone its home pad: adding or renaming an agent requires updating both files.

import java.io.IOException;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Strategies:
 * (1) At start set priority on finding food and only attack if no own
 * ant die. (2:1 => 2:0)
 * If found the enemy hill route all ants to it. Because of the safe
 * attacking strategy our ants should
 * get together in front of the hill and only will move forward if we
 * are more than the enemy, so we can capture the hill.
 * => win :)
 * (2) Search for food and connects near ants to pairs. So we will get
 * big groups of ants which have a higher chance to survive in matches.
 * 
 * 
 * Efficiency:
 * (1) Shortest Paths Algorithm - Dijkstra
 * (2) Swarm intelligence (
 * http://en.wikipedia.org/wiki/Swarm_intelligence )
 * (3) ACO - Ant colony optimization algorithms (
 * http://en.wikipedia.org/wiki/Ant_colony_optimization )
 * (4) Searching for better data-structures
 */
public class MyBot extends Bot
{
    /**
     * Main method executed by the game engine for starting the bot.
     * 
     * @param args
     *            command line arguments
     * @throws IOException
     *             if an I/O error occurs
     */
    public static void main(String[] args) throws IOException
    {
	new MyBot().readSystemInput();
    }

    /**
     * used to keep track of turnNumber (for logging)
     */
    private int currentTurn;

    /**
     * % of tiles that have been explored. if >= 100 exploretracking is
     * disabled.
     */
    private int exploredPercentage;

    /**
     * total number of tiles on map
     */
    private int totalTiles;

    /**
     * % of tiles that are visible. updated each turn.
     */
    private int visiblePercentage;

    /**
     * States whether map is fully explored. If so, exploring does not need to
     * be tracked any more.
     */
    private boolean isMapFullyExplored;

    /**
     * used for logging
     */
    private Logger log;

    /**
     * used for game data
     */
    private Ants game;

    /**
     * List of all new moves for the ants
     * Key = Location to move to
     * Value = Moving Ant
     */
    private Map<Tile, Tile> orders = new HashMap<Tile, Tile>();

    /**
     * List of seen Tiles. Used to keep track of the map.
     * null = not seen yet
     */
    private TileType[][] exploredTiles;

    /**
     * List of visible tiles.
     */
    private Set<Tile> visibleTiles;

    /**
     * Set of titles we are not explored yet
     */
    private Set<Tile> unseenTiles;

    /**
     * Set of enemy hills tiles
     */
    private Set<Tile> enemyHills = new HashSet<Tile>();

    /**
     * Map of calculated AStarRoutes
     */
    private Map<Tile, IRoute> calculatedRoutes;

    /**
     * Food tiles which are targets.<br />
     * Key = ant (must be updated after move)<br />
     * Value = food
     */
    private HashMap<Tile, Tile> foodTargets;

    /**
     * Enemy hill tiles which are targets.<br />
     * Key = ant (must be updated after move)<br />
     * Value = food
     */
    private HashMap<Tile, Tile> hillTargets;

    private List<HashMap<Tile, Tile>> targetsList;

    private TreeSet<Tile> sortedAnts;

    private ArrayList<Tile> collectedFoods;

    private Set<Tile> myPerceivedAnts;

    private List<Tile> casualties;

    private boolean turnTwoInited;

    private DecimalFormat df;

    /**
     * Constructor init basic things
     */
    public MyBot()
    {
	// init basic units
	currentTurn = 0;
	exploredPercentage = 0;
	visiblePercentage = 0;
	isMapFullyExplored = false;
	turnTwoInited = false;

	targetsList = new LinkedList<HashMap<Tile, Tile>>();
	foodTargets = new HashMap<Tile, Tile>();
	hillTargets = new HashMap<Tile, Tile>();

	collectedFoods = new ArrayList<Tile>();
	myPerceivedAnts = new HashSet<Tile>();
	casualties = new ArrayList<Tile>();

	targetsList.add(foodTargets);
	targetsList.add(hillTargets);

	df = new DecimalFormat(",##0");

	// init logger with specific formatting
	log = Logger.getAnonymousLogger();
	for (Handler pHandler : log.getParent().getHandlers())
	{
	    log.getParent().removeHandler(pHandler);
	}

	Handler handler = new ConsoleHandler();
	handler.setFormatter(new Formatter()
	{

	    @Override
	    public String format(LogRecord event)
	    {
		return "\nTurn " + currentTurn + ": " + event.getSourceMethodName() + ": " + event.getMessage()
			+ " remaining time: " + game.getTimeRemaining() + ", Free memory: "
			+ df.format(Runtime.getRuntime().freeMemory());
	    }
	});
	log.addHandler(handler);
    }

    private void attackEnemyHills()
    {
	if (isOwnArmyStronger(true, 2))
	{
	    List<Route> hillRoutes = new ArrayList<Route>();
	    for (Tile enemyHill : enemyHills)
	    {
		for (Tile ant : sortedAnts)
		{
		    if (!hasAntOrder(ant))
		    {
			// try direct way (Manhattan)
			IRoute route = new ManhattanRoute(game, ant, enemyHill, false, true, exploredTiles, log);
			boolean hasDirectPath = route.findRoute();

			// direct way found
			if (hasDirectPath)
			{
			    calculatedRoutes.put(ant, route);
			    hillTargets.put(ant, enemyHill);
			    executeStoredRoute(ant, hillTargets, null);
			}

			// no direct way found, try indirect (A*) instead
			else
			{
			    log.info("no direct path found from " + Util.tilePositionAsString(route.getStart())
				    + " to "
				    + Util.tilePositionAsString(route.getEnd()) + ". Trying indirect path (A*)..");
			    route = new AStarRoute(game, ant, enemyHill, false, exploredTiles, log);

			    // indirect way found
			    if (route.findRoute())
			    {
				calculatedRoutes.put(ant, route);
				hillTargets.put(ant, enemyHill);
				executeStoredRoute(ant, hillTargets, null);
			    }
			    // no indirect way found either
			    else
			    {
			    }
			}
			//
			// int distance = game.getDistance(ant, enemyHill);
			//
			// Route route = new Route(ant, enemyHill, distance);
			// hillRoutes.add(route);
			// log.info("attacking enemy hill");
		    }
		}
	    }
	    Collections.sort(hillRoutes);
	    for (Route route : hillRoutes)
	    {
		doMoveLocation(route.getStart(), route.getEnd());
	    }
	}
    }

    /**
     * Calculates which ants have died after the last turn and lists them. This
     * is done by comparing a list of perceived ants with the real list of ants.
     */
    private void calculateCasulties()
    {
	if (myPerceivedAnts != null)
	{
	    log.info("perceived: " + myPerceivedAnts.size() + "; real " + game.getMyAnts().size());

	    for (Tile ant : myPerceivedAnts)
	    {
		if (!game.getMyAnts().contains(ant))
		{
		    log.info("dead ant at " + Util.tilePositionAsString(ant));
		    casualties.add(ant);
		}

	    }

	    myPerceivedAnts.clear();

	}

    }

    /**
     * Checks if move is valid for an ant
     * 
     * Prevent ants from moving onto other ants
     * Prevent 2 ants from moving to the same destination
     * Track information about where all our ants are going
     * 
     * @param antLoc
     *            Location of the ant
     * @param direction
     *            Direction we want to move the ant
     * @return If ant can move to this position
     */
    private boolean doMoveDirection(Tile antLoc, Aim direction)
    {
	Tile newLoc = game.getTile(antLoc, direction);
	// int loopCounter = 0;
	// int closestEnemyDistance = getClosestEnemyDistance(antLoc);
	// int closestEnemyDistance2 = getClosestEnemyDistance(newLoc);
	//
	// if (!isOwnArmyStronger(true))
	// {
	// if (closestEnemyDistance > -1 || closestEnemyDistance2 > -1)
	// // log.info(closestEnemyDistance + ", " +
	// // closestEnemyDistance2);
	//
	// if (closestEnemyDistance2 < closestEnemyDistance)
	// {
	// // log.info("Gefahr bei " + antLoc.getRow() + ", " +
	// // antLoc.getCol());
	// return false;
	// }
	//
	// }

	// Track all moves, prevent collisions
	if (game.getIlk(newLoc).isUnoccupied() && !orders.containsKey(newLoc))
	{
	    game.issueOrder(antLoc, direction);
	    orders.put(newLoc, antLoc);
	    myPerceivedAnts.add(newLoc);
	    return true;
	}
	else
	{
	    return false;
	}
    }

    /**
     * Check if an ant already is on the way to the destination
     * 
     * @param antLoc
     *            Location of the ant
     * @param destLoc
     *            Location of the destination
     * @return If the route is still free
     */
    private boolean doMoveLocation(Tile antLoc, Tile destLoc)
    {
	// Track targets to prevent 2 ants to the same location
	List<Aim> directions = game.getDirections(antLoc, destLoc);
	Collections.shuffle(directions);
	for (Aim direction : directions)
	{
	    if (doMoveDirection(antLoc, direction))
	    {
		return true;
	    }
	}
	return false;
    }

    /**
     * For every ant check every direction in fixed order (N, E, S, W) and move
     * it if the tile is passable.
     */
    @Override
    public void doTurn()
    {

	// Do some initializing
	initEveryTurn();

	log.info("------------start of turn #" + currentTurn + "-------------");

	// update info about visible tiles
	updateVisibleTiles();

	// list casualties
	calculateCasulties();

	// track explored tiles (if map is not fully explored yet)
	if (!isMapFullyExplored)
	    trackExploring();

	// check if there are calculatedRoutes for every ant
	executeStoredRoutes();

	// check if there is any food visible
	findFood();

	// add new hills to set
	findEnemyHills();

	// attack hills
	attackEnemyHills();

	// explore unseen areas
	exploreMap();

	// prevent stepping on own hill
	preventSteppingOnOwnHill();

	// unblock hills
	manageOwnHills();

	// if (currentTurn % 10 == 0)
	// // // if (currentTurn % (game.getTurns() / 10) == 0)
	// logExploredMap();

	// add infos about ants without orders
	trackUnemployedAnts();

	// update target lists (collected food etc.)
	updateTargetLists();

	log.info("# ants " + myPerceivedAnts.size() + ", " + game.getMyAnts().size());
	log.info("------------end of turn #" + currentTurn + "-------------\n");
    }

    private void executeStoredRoute(Tile ant, HashMap<Tile, Tile> targetMap, ArrayList<Tile> toBeDeleted)
    {
	// log.info("ant at" + Util.tilePositionAsString(ant));
	if (calculatedRoutes.containsKey(ant) && targetMap.containsKey(ant) && game.getMyAnts().contains(ant))
	{
	    IRoute route = calculatedRoutes.get(ant);
	    // if (game.getFoodTiles().contains(aStarRoute.getEnd()))
	    {
		Aim direction = route.getDirection();
		if (direction != null)
		{
		    if (route.isFoodRoute())
		    {
			if (game.getFoodTiles().contains(route.getEnd()))
			{
			    if (doMoveDirection(ant, route.getDirection()))
			    {
				log.info("moving ant from " + Util.tilePositionAsString(ant) + " to "
					+ game.getTile(ant, direction));
				route.update();
				if (route.isFinished())
				{
				    collectedFoods.add(ant);
				    // if (toBeDeleted != null)
				    // toBeDeleted.add(route.getEnd());
				    // else
				    // targetMap.remove(route.getEnd());
				    log.info("finished foodroute to "
					    + Util.tilePositionAsString(route.getEnd()));
				    calculatedRoutes.put(ant, null);
				    calculatedRoutes.remove(ant);
				}
				else
				{
				    log.info("foodroute not finished..updateding maps");
				    Tile newKey = game.getTile(ant, direction);
				    calculatedRoutes.put(newKey, route);
				    calculatedRoutes.remove(ant);
				    targetMap.put(newKey, route.getEnd());
				    targetMap.remove(ant);
				    // log.info("finished updateding; contains old: "
				    // + targetMap.containsKey(ant)
				    // + ", contains new: " +
				    // targetMap.containsKey(newKey) +
				    // " # in targetMap "
				    // + targetMap.size());
				}

			    }
			    else
			    {
				orders.put(ant, null);
				log.info("ant at " + Util.tilePositionAsString(ant) + " not moving, no changes on maps");
			    }
			}
			else
			{
			    log.info("protest from " + Util.tilePositionAsString(ant) + " : food at "
				    + Util.tilePositionAsString(route.getEnd())
				    + " has been eaten already");
			    collectedFoods.add(ant);
			    // targetMap.remove(route.getEnd());
			    calculatedRoutes.put(ant, null);
			    calculatedRoutes.remove(ant);
			}
		    }
		    else
		    {
			if (doMoveDirection(ant, route.getDirection()))
			{
			    // log.info("got instructions from aStarRoute...moving out");
			    route.update();
			    if (route.isFinished())
			    {
				if (toBeDeleted != null)
				    toBeDeleted.add(ant);
				else
				    targetMap.remove(ant);
				log.info("finished route to "
					+ Util.tilePositionAsString(route.getEnd()));
				calculatedRoutes.put(ant, null);
				calculatedRoutes.remove(ant);
			    }
			    else
			    {
				// log.info("1");
				Tile newKey = game.getTile(ant, direction);
				// log.info("2");
				calculatedRoutes.put(newKey, route);
				// log.info("3");
				calculatedRoutes.remove(ant);
				// log.info("4");
				targetMap.put(newKey, route.getEnd());

				targetMap.remove(ant);
				// log.info("5");
			    }

			}
			//
			// if (doMoveDirection(ant, route.getDirection()))
			// {
			// log.info("got instructions from aStarRoute...moving out");
			// route.update();
			// Tile newKey = game.getTile(ant, direction);
			// calculatedRoutes.put(newKey, route);
			// calculatedRoutes.remove(ant);
			// }
		    }

		}
	    }

	}
	else
	{
	    log.info("Error: ant at " + Util.tilePositionAsString(ant) + " not found " +
		    calculatedRoutes.containsKey(ant) + ", " +
		    targetMap.containsKey(ant) + ", " + game.getMyAnts().contains(ant));
	}
    }

    private void executeStoredRoutes()
    {
	if (calculatedRoutes.size() > 0)
	{
	    int tmC = 0;

	    for (HashMap<Tile, Tile> targetMap : targetsList)
	    {
		tmC++;
		log.info(targetMap.size() + " ant(s) in targetMap # " + tmC);

		int targetC = 0;
		ArrayList<Tile> toBeDeleted = new ArrayList<Tile>();
		Object[] ants = targetMap.keySet().toArray();
		Tile ant;
		for (int i = 0; i < ants.length; i++)
		{
		    ant = (Tile) ants[i];
		    if (casualties.contains(ant))
		    {
			targetMap.remove(ant);
			calculatedRoutes.remove(ant);
		    }

		    else
			executeStoredRoute((Tile) ants[i], targetMap, toBeDeleted);
		}
		//
		// for (Tile ant : targetMap.keySet())
		// {
		// targetC++;
		// log.info("inspection: " + tmC + ", " + targetC);
		// executeStoredRoute(ant, targetMap, toBeDeleted);
		// }

		int deleteC = 0;
		for (Tile delete : toBeDeleted)
		{
		    deleteC++;
		    log.info("deletion: " + tmC + ", " + deleteC);
		    targetMap.remove(delete);
		}
	    }
	}
	else
	{
	    log.info("no calculated routes");
	}
    }

    private void exploreMap()
    {
	// TODO spread ants according to division of the map
	for (Tile ant : sortedAnts)
	{
	    if (!hasAntOrder(ant))
	    {
		List<Route> unseenRoutes = new ArrayList<Route>();
		for (Tile unseenLoc : unseenTiles)
		{
		    int distance = calculateDistance(ant, unseenLoc);
		    Route route = new Route(ant, unseenLoc, distance);
		    unseenRoutes.add(route);
		}
		Collections.sort(unseenRoutes);
		for (Route route : unseenRoutes)
		{
		    if (doMoveLocation(route.getStart(), route.getEnd()))
		    {
			// log.info("ant at " + Util.tilePositionAsString(ant) +
			// " exploring "
			// + Util.tilePositionAsString(route.getEnd()));
			break;
		    }
		}
	    }
	}
    }

    private void findEnemyHills()
    {
	for (Tile enemyHill : game.getEnemyHills())
	{
	    if (!enemyHills.contains(enemyHill))
	    {
		enemyHills.add(enemyHill);
		if (exploredTiles[enemyHill.getRow()][enemyHill.getCol()] == null
			|| exploredTiles[enemyHill.getRow()][enemyHill.getCol()] == TileType.LAND)
		    exploredTiles[enemyHill.getRow()][enemyHill.getCol()] = TileType.ENEMY_HILL;
	    }
	}
    }

    private void findFood()
    {
	// check if any food is visible
	if (game.getFoodTiles().size() > 0)
	{
	    // find close food
	    List<Route> foodRoutes = new ArrayList<Route>();
	    TreeSet<Tile> sortedFood = new TreeSet<Tile>(game.getFoodTiles());

	    // find closest ant for every food
	    for (Tile foodLoc : sortedFood)
	    {
		if (!foodTargets.containsValue(foodLoc))
		{
		    for (Tile ant : sortedAnts)
		    {
			if (!hasAntOrder(ant))
			{
			    int distance = calculateDistance(ant, foodLoc);
			    if (distance < 20)
			    {
				Route route = new Route(ant, foodLoc, distance);
				foodRoutes.add(route);
			    }
			    else
			    {
				// log.info("food at " +
				// Util.tilePositionAsString(foodLoc) +
				// " too far from ant at "
				// + Util.tilePositionAsString(ant) + ": " +
				// distance);
			    }

			}
			else
			{
			    // log.info("ant at " +
			    // Util.tilePositionAsString(ant) +
			    // " too busy with "
			    // + Util.getKeybyValue(orders, ant)
			    // + " to care about food at " + foodLoc);
			}
		    }
		}
	    }
	    Collections.sort(foodRoutes);

	    // find way to food
	    for (Route foodRoute : foodRoutes)
	    {
		Tile ant = foodRoute.getStart();
		Tile food = foodRoute.getEnd();

		// check if ant is not busy and food not searched by other ant
		if (!foodTargets.containsValue(food) && !hasAntOrder(ant))
		{
		    log.info("ant at " + Util.tilePositionAsString(ant) + " trying to get food at "
			    + Util.tilePositionAsString(food));

		    // try direct way (Manhattan)
		    IRoute route = new ManhattanRoute(game, ant, food, true, true, exploredTiles, log);
		    boolean hasDirectPath = route.findRoute();

		    // direct way found
		    if (hasDirectPath)
		    {
			calculatedRoutes.put(ant, route);
			foodTargets.put(ant, food);
			executeStoredRoute(ant, foodTargets, null);
		    }

		    // no direct way found, try indirect (A*) instead
		    else
		    {
			log.info("no direct path found from " + Util.tilePositionAsString(route.getStart()) + " to "
				+ Util.tilePositionAsString(route.getEnd()) + ". Trying indirect path (A*)..");
			route = new AStarRoute(game, ant, food, true, exploredTiles, log);

			// indirect way found
			if (route.findRoute())
			{
			    calculatedRoutes.put(ant, route);
			    foodTargets.put(ant, food);
			    executeStoredRoute(ant, foodTargets, null);
			}
			// no indirect way found either
			else
			{
			}
		    }

		}
	    }
	}
	// no food visible
	else
	{
	    log.info("and not a single food was found that turn");
	}
    }
    private int getClosestEnemyDistance(Tile antLoc)
    {
	Set<Tile> enemies = getEnemyAntsInRange(antLoc, game.getAttackRadius2() * 4);
	// log.info(enemies.size() + " enemies found");
	if (enemies.size() < 1)
	    return -1;
	else
	{
	    int closestDistance = Integer.MAX_VALUE;
	    for (Tile enemy : enemies)
	    {
		int distance = calculateDistance(antLoc, enemy);
		if (distance < closestDistance)
		    closestDistance = distance;
	    }
	    return closestDistance;
	}
    }

    /**
     * Calculates Distance between two tiles. Same as in <code>Ants()</code> but
     * not squared.
     * 
     * @return distance (<code>rowDelta + colDelta</code>)
     */
    private int calculateDistance(Tile t1, Tile t2)
    {
	int rowDelta = Math.abs(t1.getRow() - t2.getRow());
	int colDelta = Math.abs(t1.getCol() - t2.getCol());
	rowDelta = Math.min(rowDelta, game.getRows() - rowDelta);
	colDelta = Math.min(colDelta, game.getCols() - colDelta);

	return rowDelta + colDelta;
    }

    private Set<Tile> getEnemyAntsInRange(Tile antLoc, int range)
    {
	Set<Tile> enemies = new HashSet<Tile>(game.getEnemyAnts());
	ArrayList<Tile> toBeDeleted = new ArrayList<Tile>();
	for (Tile enemy : enemies)
	{
	    if (calculateDistance(enemy, antLoc) > range)
		toBeDeleted.add(enemy);
	}
	for (Tile delete : toBeDeleted)
	{
	    enemies.remove(delete);
	}

	return enemies;
    }

    private boolean hasAntOrder(Tile ant)
    {
	return orders.containsValue(ant);
    }

    private void initEveryTurn()
    {
	// increase turnCounter
	currentTurn++;

	// init stuff that needs reference to getAnts() (on first turn only)
	if (currentTurn == 1)
	    initTurnOne();
	else
	    if (currentTurn == 2 && !turnTwoInited)
		initTurnTwo();

	// clear orders
	orders.clear();

	// clear casualties

	// reset sortedAnts
	sortedAnts = new TreeSet<Tile>(game.getMyAnts());
    }
    /**
     * called on first turn because getAnts() does not return anything in
     * constructor
     */
    private void initTurnOne()
    {
	// save ants reference
	game = getAnts();

	// init tile Lists
	exploredTiles = new TileType[game.getRows()][game.getCols()];
	visibleTiles = new HashSet<Tile>();
	calculatedRoutes = new HashMap<Tile, IRoute>();
	totalTiles = game.getRows() * game.getCols();

	// add all locations to unseen tiles set and mark as null in seenTiles
	unseenTiles = new HashSet<Tile>();
	for (int row = 0; row < game.getRows(); row++)
	{
	    for (int col = 0; col < game.getCols(); col++)
	    {
		Tile tile = new Tile(row, col);
		visibleTiles.add(tile);
		unseenTiles.add(tile);
		exploredTiles[row][col] = null;
	    }
	}
    }

    private void initTurnTwo()
    {
	currentTurn = 1;
	turnTwoInited = true;
	for (Tile myHill : game.getMyHills())
	{
	    exploredTiles[myHill.getRow()][myHill.getCol()] = TileType.MY_HILL;
	}
    }

    private boolean isOwnArmyStronger(boolean doRegardRemainingFood, int factor)
    {
	int difference = game.getMyAnts().size() - (game.getEnemyAnts().size() * factor);

	if (doRegardRemainingFood)
	{

	    int remainingFood = game.getFoodTiles().size();
	    // log.info("difference: " + difference + ", food remaining: " +
	    // remainingFood);
	    return (difference - remainingFood) >= 1;
	}
	else
	{
	    return difference >= 1;
	}
    }

    private void logExploredMap()
    {
	String str = "";
	TileType tileType;
	for (int i = 0; i < game.getRows(); i++)
	{
	    for (int j = 0; j < game.getCols(); j++)
	    {
		tileType = exploredTiles[i][j];

		if (tileType == null)
		{
		    str += "?";
		}
		else
		{
		    switch (tileType)
		    {
			case WATER:
			    str += 0;
			    break;
			case LAND:
			    str += " ";
			    break;
			case MY_HILL:
			    str += "W";
			    break;
			case ENEMY_HILL:
			    str += "H";
			    break;
		    }
		}
	    }
	    str += "\n";
	}

	log.info(exploredPercentage + "% of Map explored, " + visiblePercentage + " % visible\n" + str);
    }

    private void manageOwnHills()
    {
	for (Tile myHill : game.getMyHills())
	{
	    if (game.getMyAnts().contains(myHill) &&
		    !orders.containsValue(myHill))
	    {
		for (Aim direction : Aim.values())
		{
		    if (doMoveDirection(myHill, direction))
		    {
			break;
		    }
		}
	    }
	}
	// TODO keep ants as guards
    }

    /**
     * Puts null into orders where the key is an own hill
     */
    private void preventSteppingOnOwnHill()
    {
	for (Tile myHill : game.getMyHills())
	{
	    orders.put(myHill, null);
	}
    }

    /**
     * Iterates trough unseen tiles and checks whether they are visible now.
     * If so, its <code>TileType</code> is stored in <code>exploredTiles</code>.
     * Own hills are tracked in <code>TurnTwoInit()</code>, enemy hills in
     * <code>findEnemyHills()</code>.
     */
    private void trackExploring()
    {
	// remove any tiles that can be seen and keep track of what kind of tile
	// they are (water, land, ownHill or enemyHill)
	for (Iterator<Tile> locIter = unseenTiles.iterator(); locIter.hasNext();)
	{
	    Tile next = locIter.next();
	    if (game.isVisible(next))
	    {
		locIter.remove();
		if (exploredTiles[next.getRow()][next.getCol()] == null)
		{
		    if (game.getIlk(next) == Ilk.WATER)
			exploredTiles[next.getRow()][next.getCol()] = TileType.WATER;
		    else
			exploredTiles[next.getRow()][next.getCol()] = TileType.LAND;
		}
	    }
	}

	exploredPercentage = (totalTiles - unseenTiles.size()) * 100
		/ (totalTiles);
	if (exploredPercentage >= 100)
	{
	    isMapFullyExplored = true;
	    log.info("MAP IS FULLY EXPLORED, NO MORE TRACKING");
	}

    }

    private void trackUnemployedAnts()
    {
	for (Tile ant : game.getMyAnts())
	{
	    if (!orders.containsValue(ant))
	    {
		log.info("ant without order at " + Util.tilePositionAsString(ant));
		myPerceivedAnts.add(ant);
	    }

	}
    }

    private void updateTargetLists()
    {
	for (Tile food : collectedFoods)
	{
	    log.info("deleting key (ant) at " + Util.tilePositionAsString(food));
	    foodTargets.remove(food);
	}

	collectedFoods.clear();
    }

    /**
     * 
     */
    private void updateVisibleTiles()
    {
	int numVisible = 0;
	for (Iterator<Tile> tileIter = visibleTiles.iterator(); tileIter.hasNext();)
	{
	    Tile next = tileIter.next();
	    if (game.isVisible(next))
		numVisible++;
	}
	visiblePercentage = (numVisible * 100) / totalTiles;
    }

}

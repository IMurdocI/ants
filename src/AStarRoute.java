import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Logger;

/**
 * A* Route makes use of the A* algorithm. It always finds the best solution (if
 * existing), but it needs more memory and time. So it should only be used if
 * there is no direct way.
 * 
 * @author Philip
 * 
 */
public class AStarRoute extends AbstractRoute
{
    /**
     * @see AStarRoute
     */
    public AStarRoute(Ants game, Tile start, Tile end, boolean isFoodRoute, TileType[][] exploredTiles, Logger log)
    {
	super.game = game;
	super.start = start;
	super.current = start;
	super.end = end;
	super.isFoodRoute = isFoodRoute;
	super.exploredTiles = exploredTiles;
	super.log = log;

	super.counter = 0;
	super.isFinished = false;
    }

    public boolean findRoute()
    {
	// A* pathfinding
	AStar pathFinder = new AStar(game, start, end, exploredTiles, log);
	path = pathFinder.findPath();
	boolean ret = path != null;
	if (ret)
	{
	    // remove first element of path since it's the start
	    path.remove(0);

	    // cut off last step if foodRoute
	    if (isFoodRoute)
		path.remove(path.size() - 1);
	}
	String str = (ret) ? "possible in " + (path.size() + 1) + " moves" : "impossible";
	log.info("path from " + start.getRow() + ", " + start.getCol() + " to " + end.getRow() + ", " + end.getCol()
		+ " is " + str);

	return ret;
    }

    private class AStar
    {
	private class Path implements Comparable
	{
	    public Tile lastTile;
	    public int f;
	    public int g;
	    public Path parent;

	    public Path()
	    {
		lastTile = null;
		g = 0;
		f = 0;
		parent = null;
	    }

	    /**
	     * Copy constructor
	     * 
	     * @param p
	     *            The path object to clone.
	     */
	    public Path(Path p)
	    {
		this();
		parent = p;
		g = p.g;
		f = p.f;
	    }

	    /**
	     * Compare to another object using the total cost f.
	     * 
	     * @param o
	     *            The object to compare to.
	     * @see Comparable#compareTo()
	     * @return <code>less than 0</code> This object is smaller
	     *         than <code>0</code>; <code>0</code> Object are the same.
	     *         <code>bigger than 0</code> This object is bigger
	     *         than o.
	     */
	    public int compareTo(Object o)
	    {
		Path p = (Path) o;
		return (int) (f - p.f);
	    }

	    /**
	     * Get the last tile on the path.
	     * 
	     * @return The last point visited by the path.
	     */
	    public Tile getLastTile()
	    {
		return lastTile;
	    }

	    /**
	     * Set the last tile on the path.
	     */
	    public void setLastTile(Tile tile)
	    {
		lastTile = tile;
	    }
	}

	private Tile start;
	private Tile end;
	private Ants game;
	private TileType[][] exploredTiles;
	private Logger log;

	/**
	 * Contains all known paths. A path may be known but not the optimal
	 * solution. If the best path to a tile is found, it is stored in the
	 * <code>closedList</code>.
	 */
	private PriorityQueue<Path> openList;

	/**
	 * Contains fully examined tiles. The minimal path to all tiles stored
	 * here
	 * is known and saved.
	 */
	private HashMap<Tile, Integer> closedList;
	private int lastGCost;
	private int expandedCounter;

	public AStar(Ants game, Tile start, Tile end, TileType[][] exploredTiles, Logger log)
	{
	    this.game = game;
	    this.start = start;
	    this.end = end;
	    this.exploredTiles = exploredTiles;
	    this.log = log;

	    openList = new PriorityQueue<Path>();
	    closedList = new HashMap<Tile, Integer>();
	    expandedCounter = 0;
	    lastGCost = 0;
	}

	/**
	 * Check if a tile is the target tile.
	 * 
	 * @param tile
	 *            The tile to check.
	 * @return <code>true</code> if it is the target,
	 *         <code>false</else> otherwise.
	 */
	private boolean isGoal(Tile tile)
	{
	    return (tile.equals(end));
	}

	/**
	 * Calculates graph (g) costs <code>to</code> from <code>from</from>.
	 * 
	 * @param src
	 *            start tile
	 * @param dest
	 *            target tile
	 * @return The cost of the operation.
	 */
	private int calculateG(Tile src, Tile dest)
	{
	    // no costs, if src == dest
	    if (src.equals(dest))
		return 0;
	    // costs depend on tileType
	    else
		return getCostsByTileType(exploredTiles[dest.getRow()][dest.getCol()]);
	}

	/**
	 * Calculates heuristic (Manhattan) distance.
	 * 
	 * @param src
	 *            start tile
	 * @param dest
	 *            target tile
	 * @return The estimated cost to reach the target tile.
	 */
	private int calculateH(Tile src, Tile dest)
	{
	    return game.getDistance(src, dest);
	}

	/**
	 * Generates the neighbours of a tile.
	 * 
	 * @param tile
	 *            The tile to get neighbours of.
	 * @return A list of neighbours.
	 */
	protected List<Tile> generateNeighbours(Tile tile)
	{
	    List<Tile> ret = new LinkedList<Tile>();

	    ret.add(game.getTile(tile, Aim.NORTH));
	    ret.add(game.getTile(tile, Aim.EAST));
	    ret.add(game.getTile(tile, Aim.SOUTH));
	    ret.add(game.getTile(tile, Aim.WEST));

	    log.info("# of neighbours for tile " + Util.tilePositionAsString(tile) + ": " + ret.size());

	    return checkNeighbours(ret);
	}

	/**
	 * Checks whether neighbours are all passable.
	 * 
	 * @param neighbours
	 *            List of Tiles generated in
	 *            <code>generateNeighbours()</code>
	 * @return List of passable tiles
	 */
	private List<Tile> checkNeighbours(List<Tile> neighbours)
	{
	    List<Tile> ret = new LinkedList<Tile>();

	    for (Tile neighbour : neighbours)
	    {
		TileType neighbourTileType = exploredTiles[neighbour.getRow()][neighbour.getCol()];
		if (neighbourTileType != null && neighbourTileType != TileType.WATER)
		    ret.add(neighbour);
	    }

	    return neighbours;
	}

	/**
	 * Check how many times a tile was expanded.
	 * 
	 * @return A counter of how many times a tiles was expanded.
	 */
	public int getExpandedCounter()
	{
	    return expandedCounter;
	}

	/**
	 * Calculates total costs (g + h)
	 * 
	 * @param path
	 *            Path from very start to <code>src</code> tile.
	 * 
	 * @param src
	 *            start tile
	 * @param dest
	 *            target tile
	 * @return The total costs.
	 */
	protected int calculateF(Path path, Tile src, Tile dest)
	{
	    int g = calculateG(src, dest) + ((path.parent != null) ? path.parent.g : 0);
	    int h = calculateH(src, dest);

	    path.g = g;
	    path.f = g + h;

	    return path.f;
	}

	/**
	 * Expand a path.
	 * 
	 * @param path
	 *            The path to expand.
	 */
	private void expand(Path path)
	{
	    Tile tile = path.getLastTile();
	    Integer minCosts = closedList.get(tile);

	    /*
	     * Only expand the existing path if it is null or the new one is shorter.
	     */
	    if (minCosts == null || minCosts.intValue() > path.f)
	    {
		closedList.put(path.getLastTile(), path.f);

		List<Tile> neighbours = generateNeighbours(tile);

		for (Tile neighbour : neighbours)
		{
		    Path newPath = new Path(path);
		    newPath.setLastTile(neighbour);
		    calculateF(newPath, path.getLastTile(), neighbour);
		    openList.offer(newPath);
		}

		expandedCounter++;
	    }
	}

	/**
	 * Find the shortest path to a goal starting from <code>start</code>.
	 * 
	 * @return A list of nodes from the initial point to a goal,
	 *         <code>null</code> if a path doesn't exist.
	 */
	public List<Tile> findPath()
	{
	    try
	    {
		Path root = new Path();
		root.setLastTile(start);

		/* Needed if the initial point has a cost.  */
		calculateF(root, start, start);

		// start with root tile
		expand(root);

		int lc = 1;
		while (game.getTimeRemaining() > 900 && !openList.isEmpty())// for
									    // (;;)
		{
		    Path path = openList.poll();
		    log.info("loop # : " + lc);
		    lc++;

		    if (path == null)
		    {
			lastGCost = Integer.MAX_VALUE;
			return null;
		    }

		    Tile lastTile = path.getLastTile();

		    lastGCost = path.g;

		    // if target is reached, build shortest path from target to
		    // root
		    // and return it
		    if (isGoal(lastTile))
		    {
			LinkedList<Tile> retPath = new LinkedList<Tile>();

			for (Path i = path; i != null; i = i.parent)
			{
			    retPath.addFirst(i.getLastTile());
			}

			return retPath;
		    }

		    // target not reached yet, so expand path
		    expand(path);
		}
	    }
	    catch (Exception e)
	    {
		e.printStackTrace();
	    }

	    // no path found
	    return null;
	}
	/**
	 * Get the Costs for a certain tile by its <code>TileType</code>
	 * 
	 * @param tile
	 *            The tile to check
	 * @return Costs for the TileType as <code>int</code>
	 */
	private int getCostsByTileType(TileType tileType)
	{
	    if (tileType == null)
		return 10;
	    else
	    {
		switch (tileType)
		{
		    case LAND:
			return 10;
		    case WATER:
			return Integer.MAX_VALUE;
		    default:
			return 10;
		}
	    }
	}
    }

}

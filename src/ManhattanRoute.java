import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * ManhattanRoute is a direct route from one <code>Tile</code> to another. Since
 * for a given route of n rows and k cols there are (n+k)!/n!/k! different
 * direct ways it is always likely to find one. Even if some of the tiles on the
 * way are not passable.
 * 
 * @author Philip
 * 
 */
public class ManhattanRoute extends AbstractRoute
{
    private List<List<Tile>> paths;
    private boolean isGreedy;

    private Aim AimHor;
    private Aim AimVert;

    /**
     * @see ManhattanRoute
     */
    public ManhattanRoute(Ants game, Tile start, Tile end, boolean isFoodRoute, boolean isGreedy,
	    TileType[][] exploredTiles, Logger log)
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

	this.isGreedy = isGreedy;
    }

    public boolean findRoute()
    {
	// TODO make use of teleportation on edges of map
	int numVert = end.getRow() - start.getRow();
	int numHor = end.getCol() - start.getCol();

	AimHor = (numHor >= 0) ? Aim.EAST : Aim.WEST;
	AimVert = (numVert >= 0) ? Aim.SOUTH : Aim.NORTH;

	numVert = Math.abs(numVert);
	numHor = Math.abs(numHor);

	int numHor2 = game.getRows() - numVert;
	int numVert2 = game.getCols() - numHor;

	if (numHor2 < numHor)
	{
	    AimHor = Util.getOppositeDirection(AimHor);
	    log.info("wrapping horizontal + " + Util.tilePositionsFromToAsString(start, end));
	}

	if (numVert2 < numVert)
	{
	    AimVert = Util.getOppositeDirection(AimVert);
	    log.info("wrapping vertical + " + Util.tilePositionsFromToAsString(start, end));
	}

	numVert = Math.min(numVert, game.getRows() - numVert);
	numHor = Math.min(numHor, game.getCols() - numHor);

	List<Tile> startPath = new LinkedList<Tile>();
	paths = new LinkedList<List<Tile>>();
	try
	{
	    recFindAllDirectPaths(start, 0, 0, numHor, numVert, startPath);
	}
	catch (GreedyConditionMetException e)
	{
	    log.info("path found...search stopped");
	}
	finally
	{
	    // at least one path found
	    if (paths.size() > 0)
	    {
		// choose one by random
		Collections.shuffle(paths);
		path = paths.get(0);

		// cut off last step if it is a foodRoute
		if (isFoodRoute && path.size() > 1)
		    path.remove(path.size() - 1);

		log.info("found direct path of length " + path.size() + ": " + Util.printPath(game, start, path));
	    }
	    // no path found
	    else
	    {
		path = null;
	    }
	}

	return (path != null);
    }
    private void recFindAllDirectPaths(Tile tile, int numHorizontal, int numVertical, int limitHorizontal,
	    int limitVertical, List<Tile> path) throws GreedyConditionMetException
    {
	if (numHorizontal == limitHorizontal && numVertical == limitVertical)
	{
	    paths.add(path);
	    if (isGreedy)
		throw new GreedyConditionMetException();
	}
	else
	{
	    if (numHorizontal < limitHorizontal)
	    {
		LinkedList<Tile> newPath = new LinkedList<Tile>(path);
		Tile nextTile = game.getTile(tile, AimHor);
		if (exploredTiles[nextTile.getRow()][nextTile.getCol()] != TileType.WATER)
		{
		    newPath.add(nextTile);
		    recFindAllDirectPaths(nextTile, numHorizontal + 1, numVertical, limitHorizontal, limitVertical,
			    newPath);
		}

	    }

	    if (numVertical < limitVertical)
	    {
		LinkedList<Tile> newPath = new LinkedList<Tile>(path);
		Tile nextTile = game.getTile(tile, AimVert);
		if (exploredTiles[nextTile.getRow()][nextTile.getCol()] != TileType.WATER)
		{
		    newPath.add(nextTile);
		    recFindAllDirectPaths(nextTile, numHorizontal, numVertical + 1, limitHorizontal, limitVertical,
			    newPath);
		}

	    }

	}

    }

    private class GreedyConditionMetException extends Exception
    {
    }
}

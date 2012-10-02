import java.util.List;
import java.util.logging.Logger;

public abstract class AbstractRoute implements IRoute
{
    protected Ants game;
    protected List<Tile> path;
    protected Tile start;
    protected Tile end;
    protected Tile current;
    protected TileType[][] exploredTiles;
    protected int counter;
    protected Logger log;

    protected boolean isFoodRoute;
    protected boolean isFinished;

    @Override
    public Aim getDirection()
    {
	if (path != null && counter < path.size())
	{
	    List<Aim> directions = game.getDirections(current, path.get(counter));
	    if (directions.size() > 0)
	    {
		Aim ret = directions.get(0);
		return ret;
	    }
	    else
	    {
		log.info("no direction found");
		return null;
	    }
	}
	else
	{
	    log.info("no path found");
	    return null;
	}
    }

    @Override
    public void update()
    {
	current = game.getTile(current, game.getDirections(current, path.get(counter)).get(0));
	counter++;

	if (counter >= path.size())
	    isFinished = true;
    }

    @Override
    public Tile getStart()
    {
	return start;
    }

    @Override
    public Tile getEnd()
    {
	return end;
    }

    @Override
    public boolean isFinished()
    {
	return isFinished;
    }

    @Override
    public boolean isFoodRoute()
    {
	return isFoodRoute;
    }

    protected void setGame(Ants game)
    {
	this.game = game;
    }
}

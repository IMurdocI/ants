import java.util.List;
import java.util.logging.Logger;

public class Ant
{
    private Logger log;
    private AntStatus antStatus;
    private Aim lastDirection;
    private Tile position;
    public Tile getPosition()
    {
	return position;
    }

    public void setPosition(Tile position)
    {
	this.position = position;
    }

    private Tile nextPosition;
    private FavDirection favDirection;
    private List<Tile> lastTwoPositions;

    public Ant(Tile tile, Logger log)
    {
	position = tile;
	this.log = log;
	favDirection = FavDirection.values()[(int) (Math.random() * FavDirection.values().length)];
	log.info("my favorite direction is : " + favDirection.toString());
    }

    private enum FavDirection
    {
	NORTH,
	NORTHEAST,
	EAST,
	SOUTHEAST,
	SOUTH,
	SOUTHWEST,
	WEST,
	NORTHWEST
    }
}

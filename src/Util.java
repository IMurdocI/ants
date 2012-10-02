import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Util
{
    public static String tilePositionAsString(Tile tile)
    {
	return tile.getRow() + ", " + tile.getCol();
    }

    public static String tilePositionsFromToAsString(Tile tile1, Tile tile2)
    {
	return "from " + tilePositionAsString(tile1) + " to " + tilePositionAsString(tile2);
    }

    public static String printPath(Ants game, Tile start, List<Tile> path)
    {
	String ret = "";

	Tile tile = start;
	for (Tile nextTile : path)
	{
	    Aim direction = game.getDirections(tile, nextTile).get(0);
	    ret += (direction.name() + ",");
	    tile = nextTile;
	}

	ret.substring(0, ret.length() - 1);
	return ret;
    }

    public static Tile getKeybyValue(Map<Tile, Tile> map, Tile value)
    {
	Tile ret = null;
	for (Tile key : map.keySet())
	{
	    if (map.get(key).equals(value))
	    {
		if (ret == null)
		    ret = key;
	    }
	}

	return ret;
    }

    public static Aim getOppositeDirection(Aim direction)
    {
	switch (direction)
	{
	    case NORTH:
		return Aim.SOUTH;
	    case EAST:
		return Aim.WEST;
	    case SOUTH:
		return Aim.NORTH;
	    case WEST:
		return Aim.EAST;
	    default:
		return Aim.NORTH;
	}
    }
}

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Util
{
    public static String tilePositionAsString(Tile tile)
    {
	return tile.getRow() + ", " + tile.getCol();
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
}

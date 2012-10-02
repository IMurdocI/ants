import java.util.List;

public interface IRoute
{
    /**
     * Get direction for next step. <code>Update()</code> should be called
     * afterwards (but only if ant really moves)
     * 
     * @return Direction to go to next.
     */
    public Aim getDirection();

    /**
     * Updates counter so the next direction can be retrieved during the
     * following round. Furthermore keeps track of whether route is finished
     * after the current move.
     */
    public void update();

    /**
     * Finds a route. Has to be implemented in concrete class.
     * 
     * @return Whether a route could be found at all.
     */
    public boolean findRoute();

    // Getters
    public Tile getStart();

    public Tile getEnd();

    public boolean isFinished();

    public boolean isFoodRoute();
}

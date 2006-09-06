//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import net.sf.gogui.util.ErrorMessage;

//----------------------------------------------------------------------------

/** Time settings.
    Time settings consist of a base time for the game and an optional
    overtime (byoyomi) for overtime periods. Overtime periods also have a
    number of moves assigned, which need to be played during an overtime
    period. The base time can be zero. If no overtime periods are used,
    the whole game must be finished in the base time.
*/
public final class TimeSettings
{
    /** Construct with total time for game.
        @param totalTime Total time for game in milliseconds.
    */
    public TimeSettings(long totalTime)
    {
        assert(totalTime > 0);
        m_preByoyomi = totalTime;
        m_byoyomi = 0;
        m_byoyomiMoves = -1;
    }

    /** Construct with base time and overtime.
        @param preByoyomi Base time for game in milliseconds.
        @param byoyomi Time for overtime period in milliseconds.
        @param byoyomiMoves Number of moves per overtime period.
    */
    public TimeSettings(long preByoyomi, long byoyomi, int byoyomiMoves)
    {
        assert(preByoyomi > 0);
        assert(byoyomi > 0);
        assert(byoyomiMoves > 0);
        m_preByoyomi = preByoyomi;
        m_byoyomi = byoyomi;
        m_byoyomiMoves = byoyomiMoves;
    }

    /** Copy constructor.
        @param timeSettings The object to be copied.
    */
    public TimeSettings(TimeSettings timeSettings)
    {
        m_preByoyomi = timeSettings.m_preByoyomi;
        m_byoyomi = timeSettings.m_byoyomi;
        m_byoyomiMoves = timeSettings.m_byoyomiMoves;
    }

    /** Get time for overtime period.
        @return Time for overtime period in milliseconds; undefined if there
        are no overtime periods in this time settings.
    */
    public long getByoyomi()
    {
        assert(getUseByoyomi());
        return m_byoyomi;
    }

    /** Get number of moves per overtime period.
        @return Number of moves per overtime period; undefined if there are
        no overtime periods in this time settings.
    */
    public int getByoyomiMoves()
    {
        assert(getUseByoyomi());
        return m_byoyomiMoves;
    }

    /** Get base time for game.
        @return Base time for game in milliseconds; this corresponds to
        the total time for the game, if there are no overtime periods.
    */
    public long getPreByoyomi()
    {
        return m_preByoyomi;
    }

    /** Check if overtime periods are used.
        @return True, if overtime periods are used in this time settings.
    */
    public boolean getUseByoyomi()
    {
        return (m_byoyomiMoves > 0);
    }

    /** Parse time settings from a string.
        The string is expected to be in the format: basetime[+byoyomi/moves]
        with base and overtime in minutes.
        @param s The string.
        @return TimeSettings The time settings corresponding to this string.
        @throws ErrorMessage On syntax error or invalid values.
    */
    public static TimeSettings parse(String s) throws ErrorMessage
    {
        boolean useByoyomi = false;
        long preByoyomi = 0;
        long byoyomi = 0;
        int byoyomiMoves = 0;
        try
        {
            int idx = s.indexOf('+');
            if (idx < 0)
            {
                preByoyomi = Long.parseLong(s) * MSEC_PER_MIN;
            }
            else
            {
                useByoyomi = true;
                preByoyomi
                    = Long.parseLong(s.substring(0, idx)) * MSEC_PER_MIN;
                int idx2 = s.indexOf('/');
                if (idx2 <= idx)
                    throw new ErrorMessage("Invalid time specification");
                byoyomi
                    = Long.parseLong(s.substring(idx + 1, idx2))
                    * MSEC_PER_MIN;
                byoyomiMoves = Integer.parseInt(s.substring(idx2 + 1));
            }
        }
        catch (NumberFormatException e)
        {
            throw new ErrorMessage("Invalid time specification");
        }
        if (preByoyomi <= 0)
            throw new ErrorMessage("Pre-byoyomi time must be positive");
        if (useByoyomi)
        {
            if (byoyomi <= 0)
                throw new ErrorMessage("Byoyomi time must be positive");
            if (byoyomiMoves <= 0)
                throw new ErrorMessage("Byoyomi moves must be positive");
            return new TimeSettings(preByoyomi, byoyomi, byoyomiMoves);
        }
        else
            return new TimeSettings(preByoyomi);
    }

    private static final long MSEC_PER_MIN = 60000L;

    private final long m_preByoyomi;

    private final long m_byoyomi;

    private final int m_byoyomiMoves;
}

//----------------------------------------------------------------------------

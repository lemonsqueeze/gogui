//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpdummy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import net.sf.gogui.util.Options;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

/** GtpDummy main function. */
public final class Main
{
    /** GtpDummy main function. */
    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "config:",
                "help",
                "log:",
                "resign:",
                "srand:",
                "version"
            };
            Options opt = Options.parse(args, options);
            if (opt.isSet("help"))
            {
                String helpText =
                    "Usage: java -jar gtpdummy.jar [options]\n" +
                    "\n" +
                    "-config    config file\n" +
                    "-help      display this help and exit\n" +
                    "-log file  log GTP stream to file\n" +
                    "-resign n  resign at n'th genmove\n" +
                    "-srand n   random seed\n" +
                    "-version   print version and exit\n";
                System.out.print(helpText);
                return;
            }
            if (opt.isSet("version"))
            {
                System.out.println("GtpDummy " + Version.get());
                return;
            }
            PrintStream log = null;
            if (opt.isSet("log"))
            {
                File file = new File(opt.getString("log"));
                log = new PrintStream(new FileOutputStream(file));
            }
            long randomSeed = 0;
            boolean useRandomSeed = false;
            if (opt.isSet("srand"))
            {
                randomSeed = opt.getLong("srand");
                useRandomSeed = true;
            }
            int resign = opt.getInteger("resign", -1);
            GtpDummy gtpDummy = new GtpDummy(log, useRandomSeed, randomSeed,
                                             resign);
            gtpDummy.mainLoop(System.in, System.out);
            if (log != null)
                log.close();
        }
        catch (Throwable t)
        {
            StringUtil.printException(t);
            System.exit(-1);
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private Main()
    {
    }
}

//----------------------------------------------------------------------------

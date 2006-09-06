//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.Options;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

/** GtpStatistics main function. */
public final class Main
{
    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "analyze:",
                "backward",
                "begin:",
                "commands:",
                "config:",
                "final:",
                "force",
                "help",
                "max:",
                "min:",
                "output:",
                "precision:",
                "program:",
                "setup",
                "size:",
                "verbose",
                "version"
            };
            Options opt = Options.parse(args, options);
            if (opt.isSet("help"))
            {
                printUsage(System.out);
                return;
            }
            if (opt.isSet("version"))
            {
                System.out.println("GtpStatistics " + Version.get());
                return;
            }
            boolean analyze = opt.isSet("analyze");
            boolean allowSetup = opt.isSet("setup");
            boolean backward = opt.isSet("backward");
            String program = "";
            if (! analyze)
            {
                if (! opt.isSet("program"))
                {
                    System.out.println("Need option -program");
                    System.exit(-1);
                }
                program = opt.getString("program");
            }
            boolean verbose = opt.isSet("verbose");
            boolean force = opt.isSet("force");
            int min = opt.getInteger("min", 0, 0);
            int max = opt.getInteger("max", Integer.MAX_VALUE, 0);
            int precision = opt.getInteger("precision", 3, 0);
            int boardSize = opt.getInteger("size", GoPoint.DEFAULT_SIZE, 1,
                                           GoPoint.MAXSIZE);
            ArrayList commands = parseCommands(opt, "commands");
            ArrayList finalCommands = parseCommands(opt, "final");
            ArrayList beginCommands = parseCommands(opt, "begin");
            ArrayList arguments = opt.getArguments();
            int size = arguments.size();
            if (analyze)
            {
                if (size > 0)
                {
                    printUsage(System.err);
                    System.exit(-1);
                }
                String fileName = opt.getString("analyze");
                String output = opt.getString("output");
                new Analyze(fileName, output, precision);
            }
            else
            {
                if (size < 1)
                {
                    printUsage(System.err);
                    System.exit(-1);
                }
                File output;
                if (opt.isSet("output"))
                    output = new File(opt.getString("output"));
                else
                    output = new File("gtpstatistics.dat");
                GtpStatistics gtpStatistics
                    = new GtpStatistics(program, arguments, output, boardSize,
                                        commands, beginCommands,
                                        finalCommands, verbose, force,
                                        allowSetup, min, max, backward);
                System.exit(gtpStatistics.getResult() ? 0 : -1);
            }
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

    private static ArrayList parseCommands(Options opt, String option)
        throws ErrorMessage
    {
        ArrayList result = null;
        if (opt.isSet(option))
        {
            String string = opt.getString(option);
            String[] array = StringUtil.split(string, ',');
            result = new ArrayList(array.length);
            for (int i = 0; i < array.length; ++i)
                result.add(array[i].trim());
        }
        return result;
    }

    private static void printUsage(PrintStream out)
    {
        out.print("Usage: java -jar gtpstatistics.jar -program program"
                  + " [options] file.sgf|dir [...]\n" +
                  "\n" +
                  "-analyze      Create HTML file from result file\n" +
                  "-begin        GTP commands to run on begin positions\n" +
                  "-backward     Iterate backward from end position\n" +
                  "-commands     GTP commands to run (comma separated)\n" +
                  "-config       Config file\n" +
                  "-final        GTP commands to run on final positions\n" +
                  "-force        Overwrite existing file\n" +
                  "-help         Display this help and exit\n" +
                  "-max          Only positions with maximum move number\n" +
                  "-min          Only positions with minimum move number\n" +
                  "-output       Filename prefix for output files\n" +
                  "-precision    Floating point precision for -analyze\n" +
                  "-setup        Allow setup stones in root position\n" +
                  "-size         Board size of games\n" +
                  "-verbose      Log GTP stream to stderr\n" +
                  "-version      Display this help and exit\n");
    }
}
    
//----------------------------------------------------------------------------

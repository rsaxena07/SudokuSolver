import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

/**
 * Main driver file, which is responsible for interfacing with the
 * command line and properly starting the backtrack solver.
 */

public class Main
{
	public static void main ( String[] args )
	{
		// Important Variables
		String file   = "";
		String var_sh = "";
		String val_sh = "";
		String cc     = "";

		for ( int i = 0; i < args.length; ++i )
		{
			String token = args[i];

			if ( token.equals( "MRV" ) )
				var_sh = "MinimumRemainingValue";

			else if ( token.equals( "MAD" ) )
				var_sh = "MRVwithTieBreaker";

			else if ( token.equals( "LCV" ) )
				val_sh = "LeastConstrainingValue";

			else if ( token.equals( "FC" ) )
				cc = "forwardChecking";

			else if ( token.equals( "NOR" ) )
				cc = "norvigCheck";

			else if ( token.equals( "TOURN" ) )
			{
				 var_sh = "tournVar";
				 val_sh = "tournVal";
				 cc     = "tournCC";
			}

			else
				file = token;
		}

		Trail trail = new Trail();

		if ( file == "" )
		{
			SudokuBoard board = new SudokuBoard( 3, 3, 7 );
			System.out.println( board.toString() );

			BTSolver solver = new BTSolver( board, trail, val_sh, var_sh, cc );
			if(cc.equals("norvigCheck") || cc.equals("forwardChecking") || cc.equals("tournCC")){
				solver.checkConsistency();
			}

			long startTime = System.nanoTime();
			solver.solve(600.0f);
			long endTime = System.nanoTime();
			float elapsedMiliSecs = ((float)(endTime - startTime)) / 1000000;

			if ( solver.hasSolution() )
			{
				System.out.println( solver.getSolution().toString() );
				System.out.println( "Trail Pushes: " + trail.getPushCount() );
				System.out.println( "Backtracks: " + trail.getUndoCount() );
				System.out.println( "Time Taken: " + elapsedMiliSecs );
			}
			else
			{
				System.out.println( "Failed to find a solution" );
			}

			return;
		}


		File location = new File ( file );
		Boolean folder = location.isDirectory();

		if ( folder )
		{
			File[] listOfBoards = location.listFiles();

			if ( listOfBoards == null )
			{
				System.out.println ( "[ERROR] Failed to open directory." );
				return;
			}

			int numSolutions = 0;
			float totalTimeTaken = 0;
			StringBuilder times = new StringBuilder();
			for ( int i = 0; i < listOfBoards.length; ++i )
			{
				//System.out.println ( "Running board: " + listOfBoards[i] );

				SudokuBoard board = new SudokuBoard( listOfBoards[i] );

				BTSolver solver = new BTSolver( board, trail, val_sh, var_sh, cc );
				if(cc.equals("norvigCheck") || cc.equals("forwardChecking") || cc.equals("tournCC")){
					solver.checkConsistency();
				}
				long startTime = System.nanoTime();
				solver.solve(600.0f);
				long endTime = System.nanoTime();
				float elapsedMiliSecs = ((float)(endTime - startTime)) / 1000000;

				if ( solver.hasSolution() ) {
					totalTimeTaken += elapsedMiliSecs;
					times.append(elapsedMiliSecs);
					times.append(",");
					numSolutions++;
				}

				trail.clear();
			}

			if (numSolutions>0) {
				times.deleteCharAt(times.length()-1);
			}

			System.out.println( "Solutions Found: " + numSolutions );
			System.out.println( "Trail Pushes: " + trail.getPushCount() );
			System.out.println( "Backtracks: "  + trail.getUndoCount() );
			System.out.println( "Average Time Taken (msecs): " + totalTimeTaken/numSolutions);
			System.out.println( "Time Values (msecs): " + times.toString());
			return;
		}

		SudokuBoard board = new SudokuBoard( location );
		System.out.println( board.toString() );

		BTSolver solver = new BTSolver( board, trail, val_sh, var_sh, cc );
		if(cc.equals("norvigCheck") || cc.equals("forwardChecking") || cc.equals("tournCC")){
			solver.checkConsistency();
		}
		long startTime = System.nanoTime();
		solver.solve(600.0f);
		long endTime = System.nanoTime();
		float elapsedMiliSecs = ((float)(endTime - startTime)) / 1000000;

		if ( solver.hasSolution() )
		{
			System.out.println( solver.getSolution().toString() );
			System.out.println( "Trail Pushes: " + trail.getPushCount() );
			System.out.println( "Backtracks: " + trail.getUndoCount() );
			System.out.println( "Time Taken (msecs): " + elapsedMiliSecs);
		}
		else
		{
			System.out.println( "Failed to find a solution" );
		}
	}
}

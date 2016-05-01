package tester;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;


/* Automated regression tester for Checkpoint 4 tests
 * Created by Max Beckman-Harned
 * updated by jfp to accommodate different project organizations
 * Put your tests in "tests/pa4_tests" folder in your Eclipse workspace directory
 * If you preface your compiler error messages / exceptions with *** then they will be displayed if they appear during processing
 */

public class Checkpoint4 {
	
	
    public static void main(String[] args) throws IOException, InterruptedException {

	// should be project directory for miniJava and tester
	String cwd = System.getProperty("user.dir");  

   
	File testDir = new File(cwd + "/../tests/pa4_tests");
	int failures = 0;
	for (File x : testDir.listFiles()) {
	    if  (x.getName().startsWith(".") || x.getName().endsWith("mJAM") || x.getName().endsWith("asm"))
		continue;
	    int returnCode = runTest(x); 
	    if (x.getName().indexOf("pass") != -1) {
		if (returnCode == 0) {
		    try {
			int val = executeTest(x);
			int expected = Integer.parseInt(x.getName().substring(5,7));
			if (val == expected)
			    System.out.println(x.getName() + " ran successfully!");
			else {
			    failures++;
			    System.err.println(x.getName() + " compiled but did not run successfully--got output " + val);
			}
		    }
		    catch(Exception ex) {
			failures++;
			System.err.println(x.getName() + " did not output correctly.");
		    }
		}
		else {
		    failures++;
		    System.err.println(x.getName()
				       + " failed to be processed!");
		}
	    } else {
		if (returnCode == 4)
		    System.out.println(x.getName() + " failed successfully!");
		else {
		    System.err.println(x.getName() + " did not fail properly!");
		    failures++;
		}
	    }
	}
	System.out.println(failures + " failures in all.");	
    }
	
    private static int runTest(File x) throws IOException, InterruptedException {

        // should be be project directory when source and class files are kept together 
        // should be "bin" subdirectory of project directory when separate src
        // and bin organization is used
        String jcp = System.getProperty("java.class.path");

	String testPath = x.getPath();
	ProcessBuilder pb = new ProcessBuilder("java", "miniJava.Compiler", testPath);
	pb.directory(new File(jcp));
	pb.redirectErrorStream(true);
	Process p = pb.start();

	processStream(p.getInputStream());
	p.waitFor();
	int exitValue = p.exitValue();
	return exitValue;
    }
	
    private static int executeTest(File x) throws IOException, InterruptedException {

        // should be be project directory when source and class files are kept together 
        // should be "bin" subdirectory of project directory when separate src
        // and bin organization is used
        String jcp = System.getProperty("java.class.path");

	String testPath = x.getPath().replace(".java", ".mJAM");
	ProcessBuilder pb = new ProcessBuilder("java", "mJAM.Interpreter", testPath);
        pb.directory(new File(jcp));
	Process process = pb.start();
		
	Scanner scan = new Scanner(process.getInputStream());
	int num = -1;
	while (scan.hasNextLine()) {
	    String line = scan.nextLine();
	    if (line.startsWith(">>> ")) {
		num = Integer.parseInt(line.substring(4));
		System.out.println("Result = " + num);
	    }
	    if (line.startsWith("*** ")) {
		System.out.println(line);
	    }
	}
	scan.close();
		
	return num;
    }
	
	
    public static void processStream(InputStream stream) {
	Scanner scan = new Scanner(stream);
	while (scan.hasNextLine()) {
	    String line = scan.nextLine();
	    if (line.startsWith("*** "))
		System.out.println(line);
	}
	scan.close();
    }
}

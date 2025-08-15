/*
 * allan amaral - 201935001
 * carlos mattos - 201935003
 */

package lang;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GenDifferentialTest {
  private String run(String... cmd) throws Exception {
    Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
    try (var in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = in.readLine()) != null) sb.append(line).append("\n");
      p.waitFor();
      return sb.toString().replace("\r\n","\n").trim();
    }
  }

  @Test
  void interpEqualsCodegen() throws Exception {
    String jasmin = System.getenv("JASMIN_JAR");
    Assumptions.assumeTrue(jasmin != null && !jasmin.isBlank(), "JASMIN_JAR not set");

    Path fatJar = Path.of("target","lang-0.1-SNAPSHOT-jar-with-dependencies.jar");
    Assumptions.assumeTrue(Files.exists(fatJar), "fat jar not built (skipping)");

    String prog = "data R{ n::Int; } " +
        "main(){ r=new R; r.n=7; iterate(i:[1,2]){ print i }; print r.n }";

    Path tmp = Files.createTempFile("prog", ".lang");
    Files.writeString(tmp, prog);

    CLI.setTestMode(false);
    String interp = run("java","-jar",fatJar.toString(),"-i",tmp.toString());

    String jasminText = run("java","-jar",fatJar.toString(),"-gen",tmp.toString());
    Path jfile = Files.createTempFile("out",".j");
    Files.writeString(jfile, jasminText);

    run("java","-jar", jasmin, jfile.toString());
    String genOut = run("java","-cp", jfile.getParent().toString(), "LangMain");

    assertEquals(interp, genOut);
  }
}



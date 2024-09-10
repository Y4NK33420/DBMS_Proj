package edu.upenn.cis.db.Neo4j;

import java.util.List;

import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.neo4j.procedure.UserFunction;

//import apoc.Dapoc.Description;

public class Neo4jSkolem {

    @UserFunction
//    @Description("example.join(['s1','s2',...], delimiter) - join the given strings with the given delimiter.")
    public String join(
            @Name("strings") List<String> strings,
            @Name(value = "delimiter", defaultValue = ",") String delimiter) {
        if (strings == null || delimiter == null) {
            return null;
        }
        return String.join(delimiter, strings);
    }
}

package org.newtglobal.dmapextension.service;

import org.springframework.stereotype.Service;

@Service
public class SolutionEngine {

	public String getSqlSolution(String oracleQueryWithoutSpaces) {
		String solutionLink = "";
		if (oracleQueryWithoutSpaces.toLowerCase().contains("instr")) {
			solutionLink = "https://www.sqlines.com/oracle-to-postgresql/instr#:~:text=In%20Oracle%20the%20INSTR%20function,with%20more%20than%202%20parameters.";
		}
		if (oracleQueryWithoutSpaces.toLowerCase().contains("insert")
				&& oracleQueryWithoutSpaces.toLowerCase().contains("into")) {
			solutionLink = "https://www.postgresql.org/docs/current/sql-insert.html";
		}
		if (oracleQueryWithoutSpaces.toLowerCase().contains("select")
				&& oracleQueryWithoutSpaces.toLowerCase().contains("from")) {
			solutionLink = "https://www.postgresql.org/docs/current/tutorial-select.html";
		}
		if (oracleQueryWithoutSpaces.toLowerCase().contains("select")
				&& oracleQueryWithoutSpaces.toLowerCase().contains("count")) {
			solutionLink = "https://www.postgresql.org/docs/8.2/functions-aggregate.html";
		}
		if (oracleQueryWithoutSpaces.toLowerCase().contains("select")
				&& oracleQueryWithoutSpaces.toLowerCase().contains("nvl(")) {
			solutionLink = "https://www.postgresql.org/docs/current/functions-conditional.html#FUNCTIONS-COALESCE-NVL-IFNULL";
		}
		if (oracleQueryWithoutSpaces.toLowerCase().contains("select")
				&& oracleQueryWithoutSpaces.toLowerCase().contains("distinct")) {
			solutionLink = "https://www.postgresql.org/docs/current/sql-select.html";
		}
		if (oracleQueryWithoutSpaces.toLowerCase().contains("select")
				&& oracleQueryWithoutSpaces.toLowerCase().contains("abs")) {
			solutionLink = "https://www.postgresql.org/docs/8.4/functions-math.html";
		}
		if (oracleQueryWithoutSpaces.toLowerCase().contains("select")
				&& oracleQueryWithoutSpaces.toLowerCase().contains("floor")) {
			solutionLink = "https://www.postgresql.org/docs/8.4/functions-math.html";
		}

		return solutionLink;
	}

}

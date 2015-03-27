package it.acubelab.main;

import org.codehaus.jettison.json.JSONObject;

import it.acubelab.BingInterface;

public class QueryBing {

	public static void main(String[] args) throws Exception {
		if (args.length >= 3)
			BingInterface.setCache(args[2]);
		BingInterface bing = new BingInterface(args[0]);
		JSONObject response = bing.queryBing(args[1]);
		System.out.println(response.toString(1));
		BingInterface.flush();
	}

}

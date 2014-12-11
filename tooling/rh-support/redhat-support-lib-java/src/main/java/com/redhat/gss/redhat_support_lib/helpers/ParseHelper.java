package com.redhat.gss.redhat_support_lib.helpers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.redhat.gss.redhat_support_lib.parsers.Link;
import com.redhat.gss.redhat_support_lib.parsers.Problem;
import com.redhat.gss.redhat_support_lib.parsers.Problems;

public class ParseHelper {
	public static List<Link> getLinksFromProblems(Problems probs){
		List<Link> links = new ArrayList<Link>();
		for(Serializable prob : probs.getSourceOrLinkOrProblem()){
			if(prob instanceof Problem){
				for(Serializable link : ((Problem) prob).getSourceOrLink()){
					if(link instanceof Link){
						links.add((Link) link);
					}
				}
			}
		}
		return links;
	}
	
	public static Properties parseConfigFile(String fileName) throws IOException{
		Properties prop = new Properties();
	    InputStream is = new FileInputStream(fileName);
	    prop.load(is);
	    return prop;
	}
}

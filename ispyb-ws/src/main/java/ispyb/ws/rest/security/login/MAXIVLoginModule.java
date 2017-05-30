package ispyb.ws.rest.security.login;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

public class MAXIVLoginModule {
	  private	static String groupUniqueMemberName = "uniqueMember";
	  private	static String principalDNSuffix = ",ou=maxivusers,dc=maxlab,dc=lu,dc=se";
	  private	static String groupCtxDN = "ou=maxivgroups,dc=maxlab,dc=lu,dc=se";
	  private	static String principalDNPrefix = "uid=";
	  private	static String groupAttributeID = "cn";
	  private	static String server = "ldap://w-v-ispybldap-0:389/";


	protected static Properties getConnectionProperties(String username, String password){
		Properties env = new Properties();
		env.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
		env.put("principalDNPrefix", principalDNPrefix);
		env.put("java.naming.security.principal", "uid=" + username + ",ou=maxivusers,dc=maxlab,dc=lu,dc=se");
		env.put("groupAttributeID", groupAttributeID);
		env.put("groupCtxDN", groupCtxDN);
		env.put("principalDNSuffix", principalDNSuffix);
		env.put("allowEmptyPasswords", "false");
		env.put("groupUniqueMember", groupUniqueMemberName);
		env.put("jboss.security.security_domain", "ispyb");
		env.put("java.naming.provider.url", server);
		env.put("java.naming.security.authentication", "simple");
		env.put("java.naming.security.credentials", password);
		return env;
		
	}
	
	protected static String getFilter(String username){
		String userDN = principalDNPrefix + username + principalDNSuffix;
		return new StringBuffer().append("(&")
				.append("(objectClass=groupOfUniqueNames)")
				.append("(" + groupUniqueMemberName + "=").append(userDN)
				.append(")").append(")").toString();
	}
	
	public static List<String> authenticate(String username, String password)
			throws Exception {
		
		List<String> myRoles = new ArrayList<String>();
		if (!password.isEmpty()){
			InitialLdapContext ctx = new InitialLdapContext(getConnectionProperties(username, password), null);
			
			// Set up search constraints
			SearchControls cons = new SearchControls();
			cons.setSearchScope(SearchControls.SUBTREE_SCOPE);
			// Search
			NamingEnumeration<SearchResult> answer = ctx.search(groupCtxDN, getFilter(username),cons);
			while (answer.hasMore()) {
				SearchResult sr = answer.next();
				Attributes attrs = sr.getAttributes();
				System.out.println(attrs.toString());
				Attribute roles = attrs.get(groupAttributeID);
				for (int r = 0; r < roles.size(); r++) {
					Object value = roles.get(r);
					String roleName = null;
					roleName = value.toString();
					// fill roles array
					if (roleName != null) {
						myRoles.add(roleName);
					}
				}
			}
			/** Any validated user is in role User */
			if (myRoles.size() == 0){
				myRoles.add("User");
			}
		}
		else{
			throw new Exception("Empty passwords are not allowed");
		}
		return myRoles;
	}
}

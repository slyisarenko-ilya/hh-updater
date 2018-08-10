package lq.hh.resume.auth.secret;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import lq.hh.exception.GetSecretException;
import lq.hh.resume.auth.ClientIdentity;

public class ConsoleSecretProvider implements SecretProvider {

	public ClientIdentity get() throws GetSecretException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
        try {
			System.out.println("CLIENT_ID и CLIENT_SECRET можно получить на https://dev.hh.ru/admin");
	        ClientIdentity identity = new ClientIdentity();
	        System.out.print("CLIENT_ID:>");
	        identity.setClientId(br.readLine());
	        System.out.print("CLIENT_SECRET:>");
	        identity.setClientSecret(br.readLine());
	        System.out.print("Логин для авторизации в headhunt:>");
	        identity.setHhUserName(br.readLine());
	        System.out.print("Пароль headhunt:>");
			identity.setHhPassword(br.readLine());
	        return identity;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new GetSecretException("Can't get client secret: ", e);
		}
	}

}

package com.twogathertales.encounterservice.controller;

import com.google.gson.Gson;
import com.twogathertales.encounterservice.model.customprincipal.CustomPrincipal;
import com.twogathertales.encounterservice.model.encounter.Encounter;
import com.twogathertales.encounterservice.service.GenericService;
import com.twogathertales.encounterservice.util.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/encounter")
public class EncounterController {

    /*TODO: Convert the GSOnConverters and any converter I can think of into classes that I send to the util package.
       Others to add too:
        -  now()
        -
    */

    @Autowired
    private GenericService<Encounter> encounterRepository;

    @Autowired
    private RestTemplate restTemplate;

    Encounter encounter;

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    public Encounter getEncounter() {
        return encounter;
    }

    public void setEncounter(Encounter encounter) {
        this.encounter = encounter;
    }

    public static String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }


    @GetMapping("/health")
    public ResponseEntity<String> health() {
        try {
            UUID uuid = UUID.randomUUID();
            String response = "OK   " + now();
            return new ResponseEntity<String>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/player")
    public ResponseEntity<String> player() {
        try {
            UUID uuid = UUID.randomUUID();
            String response = "OK   " + now();
            return new ResponseEntity<String>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        }
    }

    private void verifyPlayerServiceHealth(){
        restTemplate = new RestTemplate();
        restTemplate.getForObject("http://localhost:9011/api/player/health", String.class );
    }

    private HttpEntity<String> httpEntitySetup(String body) {
        restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Token token = Token.INSTANCE.getInstance();

        java.lang.String tokenvalue = token.getValue();
        headers.set("Authorization", "Bearer "+tokenvalue);
        return new HttpEntity<String>((String) body, headers);
    }

    private ResponseEntity<String> playerAttackRESTCallbak(Long playerID){
        restTemplate = new RestTemplate();
        HttpEntity<String> entity = httpEntitySetup("");
        return restTemplate.exchange("http://localhost:9011/api/player/attack/"+playerID, HttpMethod.GET, entity,
                String.class);
    }

    private ResponseEntity<String> playerCounterRESTCallbak(Long playerID) throws IOException {
        restTemplate = new RestTemplate();
        HttpEntity<String> entity = httpEntitySetup(encounter.calcEnemyDMG());
        return restTemplate.exchange("http://localhost:9011/api/player/counter/"+playerID, HttpMethod.POST, entity,
                String.class);
    }

    private Map convertEntityStringToObject(ResponseEntity<String> response){
        String entityString = response.getBody().toString();
        Gson gson = new Gson();
        return  gson.fromJson(entityString, Map.class);
    }

    private boolean isEncounterNull(Encounter encounter) {
        if(encounter != null)
            return true;
        encounter = new Encounter();
        return false;
    }

    private void playerAttack(Long playerID, Encounter encounter) {
        verifyPlayerServiceHealth();
        ResponseEntity<String> response = playerAttackRESTCallbak(playerID);
        isEncounterNull(encounter);
        encounter.setPlayer(convertEntityStringToObject(response));
        setEncounter(encounter);
    }

    private void enemyCounter() throws IOException {
        Encounter encounter = getEncounter();
        verifyEnemyServiceHealth();
        ResponseEntity<String> response = enemyCounterRESTCallback();
        if(response != null)
            encounter.setEnemy(convertEntityStringToObject(response));
        setEncounterEnemyID();
    }

    private ResponseEntity<String> enemyCreateRESTCallback() throws IOException {
        restTemplate = new RestTemplate();
        Encounter encounter = getEncounter();
        HttpEntity<String> entity;
        ResponseEntity<String> response;
        entity = httpEntitySetup("");
        return restTemplate.exchange("http://localhost:9012/api/enemy/create/", HttpMethod.POST, entity,
                String.class);
    }

    private ResponseEntity<String> enemyCounterRESTCallback() throws IOException {
        restTemplate = new RestTemplate();
        Encounter encounter = getEncounter();
        HttpEntity<String> entity;

        encounter.assembleEnemy(convertEntityStringToObject(enemyCreateRESTCallback()));
        entity = httpEntitySetup(encounter.calcPlayerWeaponDMG());
        setEncounter(encounter);
        return restTemplate.exchange("http://localhost:9012/api/enemy/counter/"+encounter.getEnemyID(),
                HttpMethod.POST, entity, String.class);
    }

    public void setEncounterEnemyID(){
        encounter = getEncounter();
        encounter.setEnemyID(((Double) encounter.getEnemy().get("id")).longValue());
        setEncounter(encounter);
    }

    private void playerCounter(Long playerID) throws IOException {
        ResponseEntity<String> response = playerCounterRESTCallbak(playerID);
        encounter.setPlayer(convertEntityStringToObject(response));
        setEncounter(encounter);
    }

    private void verifyEnemyServiceHealth() {
        restTemplate = new RestTemplate();
        restTemplate.getForObject("http://localhost:9012/api/enemy/health", String.class );
    }

    @GetMapping("/attack/player/{playerid}")
    @PreAuthorize("hasAnyAuthority('role_admin', 'role_user')")
    public ResponseEntity<Encounter> attack(@PathVariable("playerid") Long playerID) {
        try {
            CustomPrincipal principal = (CustomPrincipal) SecurityContextHolder.getContext().getAuthentication()
                    .getPrincipal();
            if(principal.getId().equals(String.valueOf(playerID)) ||  (principal.getUsername().equals("admin"))) {

                Encounter encounter = encounterRepository.find(playerID);
                attackActions(encounter, playerID);

                encounter = getEncounter();
                encounterRepository.save(encounter);
                return new ResponseEntity<Encounter>(encounter, HttpStatus.OK);
            }
            return new ResponseEntity<Encounter>(
                    HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return new ResponseEntity<Encounter>(
                    HttpStatus.BAD_REQUEST);
        }
    }

    private Encounter getEncounterIDAfterQuery(Encounter encounter, Long playerId){
        if(encounter == null)
            encounter = new Encounter();
        encounter.setPlayerID(playerId);

        return encounter;
    }


    private void attackActions(Encounter encounter, Long playerID) throws IOException {
        encounter = getEncounterIDAfterQuery(encounter, playerID);
        playerAttack(encounter.getPlayerID(), encounter);
        enemyCounter();
        if (this.encounter.isEnemyDead())
            return;
        playerCounter(encounter.getPlayerID());
        if (this.encounter.isPlayerDead())
            return;
        this.encounter.setState("ongoing");
    }


}
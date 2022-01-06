package com.twogathertales.encounterservice.model.encounter;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Data;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name="encounter", schema = "avarum_game")
@TypeDef(name = "json", typeClass = JsonType.class)
@Data
public class Encounter implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "state")
    private String state;
    @Column(name = "player_id")
    private Long playerID;
    @Column(name = "enemy_id")
    private Long enemyID;

    @Type(type = "json")
    private transient Map<String, Object> player = new HashMap<>();
    @Type(type = "json")
    private transient Map<String, Object> enemy = new HashMap<>();


    public Boolean isEnemyDead(){
        if((Boolean) ((Map<String, Object>) this.getEnemy()).get("dead")){
            this.state = "victory";
            return true;
        }
        return false;
    }

    public Boolean isPlayerDead(){
        if((Boolean) ((Map<String, Object>) this.getPlayer()).get("dead")) {
            this.state = "defeat";
            return true;
        }
        return false;
    }

    public String calcPlayerWeaponDMG() throws IOException {
        Object weapon =  this.getPlayer().get("weapon");
        Integer dmg = ((Double) ((Map<String, Object>) weapon).get("dmg")).intValue();
        Map<String, Integer> body_dmg = new HashMap<String, Integer>() {{
            put("dmg", dmg);
        }};
        ObjectMapper mapper = new ObjectMapper();
        return (String) mapper.writerWithDefaultPrettyPrinter().writeValueAsString(body_dmg);
    }

    public String calcEnemyDMG() throws IOException {
        Integer dmg = ((Double) ((Map<String, Object>) this.getEnemy()).get("dmg")).intValue();
        Map<String, Integer> body_dmg = new HashMap<String, Integer>() {{
            put("dmg", dmg);
        }};
        ObjectMapper mapper = new ObjectMapper();
        return (String) mapper.writerWithDefaultPrettyPrinter().writeValueAsString(body_dmg);
    }

    public Long calcEnemyIDFromEnemyObject() {
        return  ((Double) ((Map<String, Object>) this.getEnemy()).get("id")).longValue();
    }

    public void assembleEnemy(Map entityObject) throws IOException {
        if(this.getEnemyID() == null) {
            this.setEnemy(entityObject);
            this.setEnemyID(this.calcEnemyIDFromEnemyObject());
        }
    }
}

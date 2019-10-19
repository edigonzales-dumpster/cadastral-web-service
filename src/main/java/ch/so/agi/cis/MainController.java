package ch.so.agi.cis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import ch.so.geo.schema.agi.cadastralinfo._1_0.extract.Address;
import ch.so.geo.schema.agi.cadastralinfo._1_0.extract.CadastralExtract;
import ch.so.geo.schema.agi.cadastralinfo._1_0.extract.GetExtractByIdResponse;
import ch.so.geo.schema.agi.cadastralinfo._1_0.extract.ObjectFactory;
import ch.so.geo.schema.agi.cadastralinfo._1_0.extract.Office;
import ch.so.geo.schema.agi.cadastralinfo._1_0.extract.RealEstateDPR;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.ParseException;

import org.slf4j.Logger;

@Controller
public class MainController {
    private Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

    private static final String TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_LIEGENSCHAFT = "dm01vch24lv95dliegenschaften_liegenschaft";
    private static final String TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_SELBSTRECHT = "dm01vch24lv95dliegenschaften_selbstrecht";
    private static final String TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_BERGWERK = "dm01vch24lv95dliegenschaften_bergwerk";
    private static final String TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_GRUNDSTUECK = "dm01vch24lv95dliegenschaften_grundstueck";
    private static final String TABLE_SO_G_V_0180822GRUNDBUCHKREISE_GRUNDBUCHKREIS = "so_g_v_0180822grundbuchkreise_grundbuchkreis";
    private static final String TABLE_SO_G_V_0180822NACHFUEHRUNGSKREISE_GEMEINDE = "so_g_v_0180822nachfuehrngskrise_gemeinde";

    protected static final String extractNS = "http://geo.so.ch/schema/AGI/CadastralInfo/1.0/Extract";

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Value("${app.dbschema}")
    private String dbschema;

    @GetMapping("/")
    public ResponseEntity<String>  ping() {
//        logger.info("env.dburl {}", dburl);
        return new ResponseEntity<String>("cadastral-info-service", HttpStatus.OK);
    }

    @GetMapping(value="/extract/{egrid}", consumes=MediaType.ALL_VALUE, produces = {MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getExtractWithGeometryByEgrid(@PathVariable String egrid) {
        
        Grundstueck parcel = this.getParcelByEgrid(egrid);
        if(parcel==null) {
            return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
        }
        
        CadastralExtract extract = createExtract(parcel.getEgrid(), parcel);
        GetExtractByIdResponse response = new GetExtractByIdResponse();
        response.setCadastralExtract(extract);
        
        return new ResponseEntity<GetExtractByIdResponse>(response, HttpStatus.OK);
    }
    
    private CadastralExtract createExtract(String egrid, Grundstueck parcel) {
        ObjectFactory objectFactory = new ObjectFactory();
        CadastralExtract extract = new CadastralExtract();

        Office responsibleOffice = new Office();
        responsibleOffice.setName("Amt für Geoinformation");
        responsibleOffice.setOfficeAtWeb("https://agi.so.ch");
        
        Address addressResponsibleOffice = objectFactory.createAddress();
        addressResponsibleOffice.setStreet("Rötistrasse");
        addressResponsibleOffice.setNumber("4");
        addressResponsibleOffice.setPostalCode("4501");
        addressResponsibleOffice.setCity("Solothurn");
        addressResponsibleOffice.setEmail("agi@bd.so.ch");
        addressResponsibleOffice.setPhone("032 627 75 92");

        responsibleOffice.setPostalAddress(addressResponsibleOffice);
        extract.setResponsibleOffice(responsibleOffice);
        
        XMLGregorianCalendar today = null;
        try {
            GregorianCalendar gdate = new GregorianCalendar();
            gdate.setTime(new java.util.Date());
            today = DatatypeFactory.newInstance().newXMLGregorianCalendar(gdate);
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException(e);
        }
        extract.setCreationDate(today);
        
        RealEstateDPR realEstate = new RealEstateDPR();
        realEstate.setEGRID(egrid);
        realEstate.setIdentND(parcel.getNbident());
        realEstate.setSubunitOfLandRegister(parcel.getGbSubKreis());
        realEstate.setType(parcel.getArt());
        realEstate.setLandRegistryArea(Double.valueOf(parcel.getFlaechenmass()).intValue());
        
        String sql = "SELECT\n" + 
                "    gb.aname AS gb_name,\n" + 
                "    gb.amtschreiberei AS gb_amtschreiberei,\n" + 
                "    gb.amt AS gb_amt,\n" + 
                "    gb.strasse AS gb_strasse,\n" + 
                "    gb.hausnummer AS gb_hausnummer,\n" + 
                "    gb.plz AS gb_plz,\n" + 
                "    gb.ortschaft AS gb_ortschaft,\n" + 
                "    gb.telefon AS gb_telefon,\n" + 
                "    gb.email AS gb_email,\n" + 
                "    gb.web AS gb_web,\n" + 
                "    nfg.gemeindename AS gemeinde,\n" + 
                "    nfg.nfg_vorname AS nfg_vorname,\n" + 
                "    nfg.nfg_name AS nfg_name,\n" + 
                "    nfg.firma AS nfg_firma,\n" + 
                "    nfg.firma_zusatz AS nfg_firma_zusatz,\n" + 
                "    nfg.strasse AS nfg_strasse,\n" + 
                "    nfg.hausnummer AS nfg_hausnummer,\n" + 
                "    nfg.plz AS nfg_plz,\n" + 
                "    nfg.ortschaft AS nfg_ortschaft,\n" + 
                "    nfg.telefon AS nfg_telefon,\n" + 
                "    nfg.email AS nfg_email,\n" + 
                "    nfg.web AS nfg_web\n" + 
                "FROM\n" + 
                "    "+getSchema()+"."+TABLE_SO_G_V_0180822GRUNDBUCHKREISE_GRUNDBUCHKREIS+" AS gb\n" + 
                "    LEFT JOIN "+getSchema()+"."+TABLE_SO_G_V_0180822NACHFUEHRUNGSKREISE_GEMEINDE+" AS nfg\n" + 
                "    ON nfg.bfsnr = gb.bfsnr\n" + 
                "WHERE\n" + 
                "    gb.nbident = ?";
        Office landRegisterOffice = jdbcTemplate.queryForObject(sql, new RowMapper<Office>() {
            @Override
            public Office mapRow(ResultSet rs, int rowNum) throws SQLException {
                Office office = new Office();
                office.setName(rs.getString("gb_amtschreiberei"));
                Address address = new Address();
                address.setStreet(rs.getString("gb_strasse"));
                address.setNumber(rs.getString("gb_hausnummer"));
                address.setPostalCode(rs.getString("gb_plz"));
                address.setCity(rs.getString("gb_ortschaft"));
                address.setWeb(rs.getString("gb_web"));
                address.setEmail(rs.getString("gb_email"));
                address.setPhone(rs.getString("gb_telefon"));
                office.setPostalAddress(address);
                return office;
            }
        }, realEstate.getIdentND());
        
        Office surveyorOffice = jdbcTemplate.queryForObject(sql, new RowMapper<Office>() {
            @Override
            public Office mapRow(ResultSet rs, int rowNum) throws SQLException {
                Office office = new Office();
                office.setName(rs.getString("nfg_vorname") + " " + rs.getString("nfg_name"));
                Address address = new Address();
                address.setLine1(rs.getString("nfg_firma"));
                address.setLine2(rs.getString("nfg_firma_zusatz"));
                address.setStreet(rs.getString("nfg_strasse"));
                address.setNumber(rs.getString("nfg_hausnummer"));
                address.setPostalCode(rs.getString("nfg_plz"));
                address.setCity(rs.getString("nfg_ortschaft"));
                address.setWeb(rs.getString("nfg_web"));
                address.setEmail(rs.getString("nfg_email"));
                address.setPhone(rs.getString("nfg_telefon"));
                office.setPostalAddress(address);
                return office;
            }
        }, realEstate.getIdentND());

        realEstate.setLandRegisterOffice(landRegisterOffice);
        realEstate.setSurveyorOffice(surveyorOffice);
        
        try {
            java.util.Map<String,Object> municipality=jdbcTemplate.queryForMap(sql, realEstate.getIdentND());
            realEstate.setMunicipality((String)municipality.get("gemeinde"));
        } catch (EmptyResultDataAccessException e) {
            logger.warn("no municipality for nbident {}", realEstate.getIdentND());
        }

        
        
        extract.setRealEstate(realEstate);
        
        return extract;
    }
    
    
    private Grundstueck getParcelByEgrid(String egrid) {
        PrecisionModel precisionModel=new PrecisionModel(1000.0);
        GeometryFactory geomFactory=new GeometryFactory(precisionModel);
        String sql = "SELECT ST_AsBinary(l.geometrie) as l_geometrie,ST_AsBinary(s.geometrie) as s_geometrie,ST_AsBinary(b.geometrie) as b_geometrie,nummer,nbident,art,gesamteflaechenmass,l.flaechenmass as l_flaechenmass,s.flaechenmass as s_flaechenmass,b.flaechenmass as b_flaechenmass FROM "+getSchema()+"."+TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_GRUNDSTUECK+" g"
                +" LEFT JOIN "+getSchema()+"."+TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_LIEGENSCHAFT+" l ON g.t_id=l.liegenschaft_von "
                +" LEFT JOIN "+getSchema()+"."+TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_SELBSTRECHT+" s ON g.t_id=s.selbstrecht_von"
                +" LEFT JOIN "+getSchema()+"."+TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_BERGWERK+" b ON g.t_id=b.bergwerk_von"
                +" WHERE g.egris_egrid=?";        
        List<Grundstueck> gslist=jdbcTemplate.query(sql, new RowMapper<Grundstueck>() {
                    WKBReader decoder=new WKBReader(geomFactory);
                    
                    @Override
                    public Grundstueck mapRow(ResultSet rs, int rowNum) throws SQLException {
                        Geometry polygon=null;
                        byte l_geometrie[]=rs.getBytes("l_geometrie");
                        byte s_geometrie[]=rs.getBytes("s_geometrie");
                        byte b_geometrie[]=rs.getBytes("b_geometrie");
                        try {
                            if(l_geometrie!=null) {
                                polygon=decoder.read(l_geometrie);
                            }else if(s_geometrie!=null) {
                                polygon=decoder.read(s_geometrie);
                            }else if(b_geometrie!=null) {
                                polygon=decoder.read(b_geometrie);
                            }else {
                                throw new IllegalStateException("no geometrie");
                            }
                            if(polygon==null || polygon.isEmpty()) {
                                return null;
                            }
                        } catch (ParseException e) {
                            throw new IllegalStateException(e);
                        }
                        Grundstueck ret=new Grundstueck();
                        ret.setGeometrie(polygon);
                        ret.setEgrid(egrid);
                        ret.setNummer(rs.getString("nummer"));
                        ret.setNbident(rs.getString("nbident"));
                        ret.setArt(rs.getString("art"));
                        int f=rs.getInt("gesamteflaechenmass");
                        if(rs.wasNull()) {
                            if(l_geometrie!=null) {
                                f=rs.getInt("l_flaechenmass");
                            }else if(s_geometrie!=null) {
                                f=rs.getInt("s_flaechenmass");
                            }else if(b_geometrie!=null) {
                                f=rs.getInt("b_flaechenmass");
                            }else {
                                throw new IllegalStateException("no geometrie");
                            }
                        }
                        ret.setFlaechenmass(f);
                        return ret;
                    }
                }, egrid);
        if(gslist==null || gslist.isEmpty()) {
            return null;
        }
        Polygon polygons[]=new Polygon[gslist.size()];
        int i=0;
        for(Grundstueck gs:gslist) {
            polygons[i++]=(Polygon)gs.getGeometrie();
        }
        Geometry multiPolygon=geomFactory.createMultiPolygon(polygons);
        Grundstueck gs=gslist.get(0);
        gs.setGeometrie(multiPolygon);

        try {
            java.util.Map<String,Object> gbKreis=jdbcTemplate.queryForMap(
                    "SELECT aname,bfsnr FROM "+getSchema()+"."+TABLE_SO_G_V_0180822GRUNDBUCHKREISE_GRUNDBUCHKREIS+" WHERE nbident=?",gs.getNbident());
            gs.setGbSubKreis((String)gbKreis.get("aname"));
            gs.setBfsNr((Integer)gbKreis.get("bfsnr"));
        }catch(EmptyResultDataAccessException ex) {
            logger.warn("no gbkreis for nbident {}",gs.getNbident());
        }

        return gs;   
    }
    
    private String getSchema() {
        return dbschema!=null?dbschema:"xoereb";
    }
}

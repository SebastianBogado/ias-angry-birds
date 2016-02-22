package ar.fi.uba.celdas.utils;

import ar.fi.uba.celdas.ias.Action;
import ar.fi.uba.celdas.ias.IntelligentAutonomousSystem;
import ar.fi.uba.celdas.ias.TheoryCondition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by seba on 2/21/16.
 */
public class IASMarshaller {
    String filename;
    Gson gson;

    public IASMarshaller(String _filename) {
        gson = new GsonBuilder()
                .registerTypeAdapter(TheoryCondition.class, new InterfaceAdapter<TheoryCondition>())
                .registerTypeAdapter(Action.class, new InterfaceAdapter<Action>())
                .excludeFieldsWithModifiers(Modifier.TRANSIENT | Modifier.PRIVATE | Modifier.STATIC)
                .setPrettyPrinting()
                .create();

        filename = _filename;
        File file = new File(filename);
        if (!file.exists()) {
            try {
                file.createNewFile();
                save(new IntelligentAutonomousSystem());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public IntelligentAutonomousSystem getIAS() {
        try {
            String json = new String(Files.readAllBytes(Paths.get(filename)));
            return gson.fromJson(json, IntelligentAutonomousSystem.class);
        } catch (IOException e) {
            e.printStackTrace();

            return new IntelligentAutonomousSystem();
        }
    }

    public void save(IntelligentAutonomousSystem intelligentAutonomousSystem) {
        try( PrintWriter out = new PrintWriter( filename ) ){
            out.print(gson.toJson(intelligentAutonomousSystem));
        } catch (FileNotFoundException e) {
            System.out.println("ERROR while saving to " + filename + ", file not found.");
        }
    }
}

package ru.robo.voxel;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Year;
import java.util.Date;
import java.util.UUID;

//Creator: Robo_Start
//Creation date & time: 4/26/2025 11:35 AM
public class VoxParser {
    public static void main(String[] args) {
        Logger.getInstance();
        try {
            if (args.length < 1) throw new RuntimeException("No file in args");
            //Vars
            int version = 0;

            int x = 0, y = 0, z = 0;
            Voxel[] voxels = new Voxel[0];

            String filename = "";
            for (String arg : args) {
                filename += arg;
            }
            if (!Files.exists(Path.of(filename))) throw new NoSuchFileException(filename);
            Logger.getInstance().init(new File(filename).getParentFile().getPath());
            try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)))) {
                Logger.getInstance().log("Target File: " + filename);
                //Reading VOX
                byte[] voxTitle = new byte[4];
                for (int i = 0; i < 4; i++) {
                    voxTitle[i] = dis.readByte();
                }
                if (!new String(voxTitle).equals("VOX ")) throw new IOException("Bad File");
                //Reading version
                version = Integer.reverseBytes(dis.readInt());
                //Reading MAIN header
                byte[] mainChunk = new byte[4];
                for (int i = 0; i < 4; i++) {
                    mainChunk[i] = dis.readByte();
                }
                if (!new String(mainChunk).equals("MAIN")) throw new IOException("Bad MAIN Chunk");
                //Skipping 4*2 bytes in MAIN
                for (int i = 0; i < 8; i++) {
                    dis.readByte();
                }
                //Reading PACK/SIZE header
                byte[] packChunk = new byte[4];
                for (int i = 0; i < 4; i++) {
                    packChunk[i] = dis.readByte();
                }
                if (new String(packChunk).equals("PACK")) {
                    //Skipping 4*3 bytes in PACK
                    for (int i = 0; i < 12; i++) {
                        dis.readByte();
                    }
                    //Reading SIZE header
                    packChunk = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        packChunk[i] = dis.readByte();
                    }
                } else {
                    Logger.getInstance().log("PACK chunk is missing. Skipping...");
                }
                if (!new String(packChunk).equals("SIZE")) throw new IOException("Bad SIZE Chunk");
                //Skipping 4*2 bytes in SIZE
                for (int i = 0; i < 8; i++) {
                    dis.readByte();
                }
                //Reading sizes in SIZE
                x = Integer.reverseBytes(dis.readInt());
                y = Integer.reverseBytes(dis.readInt());
                z = Integer.reverseBytes(dis.readInt());


                if (x > 33 || y > 33 || z > 33) {
                    Logger.getInstance().log("The model is too big! Max 33x33x33");
                    throw new RuntimeException();
                }
                Logger.getInstance().log("Model size: " + x + "x" + y + "x" + z);
                //Reading XYZI header
                byte[] xyziChunk = new byte[4];
                for (int i = 0; i < 4; i++) {
                    xyziChunk[i] = dis.readByte();
                }
                if (!new String(xyziChunk).equals("XYZI")) throw new IOException("Bad XYZI Chunk");
                //Skipping 4*3 bytes in XYZI
                for (int i = 0; i < 12; i++) {
                    dis.readByte();
                }
                //Reading voxels in XYZI
                voxels = new Voxel[x * y * z];
                for (int i = 0; i < voxels.length; i++) {
                    voxels[i] = new Voxel(
                            dis.readByte(), //X
                            dis.readByte(), //Y
                            dis.readByte(), //Z
                            Math.abs(dis.readByte())  //Color
                    );
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            int size = (Math.max(Math.max(x, y), z) / 2);
            Logger.getInstance().log("Box size: " + size);
            Logger.getInstance().log("Total voxels count: " + voxels.length);
            String command = "return ";
            try {
                for (Voxel voxel : voxels) {
                    if (!command.equals("return ")) command += " or ";
                    command += "(x==" + (voxel.x - size) + " and y==" + (voxel.y - size) + " and z==" + (voxel.z - size) + " and " + (voxel.color) + ")";
                }
            } catch (Exception ignored) {
                command = command.replaceFirst(".$", "").replaceFirst(".$", "").replaceFirst(".$", "").replaceFirst(".$", "");
            }
            //System.out.println(command);
            File vox = new File(filename);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(String.valueOf(Path.of(vox.getParentFile().getAbsolutePath(), vox.getName().split("\\.")[0] + "-Replicube.vox"))))) {
                String content = "{\n";
                content += "\"animated\": false,\n";
                content += "\"code\": \"" + command + "\",\n";
                content += "\"id\": \"" + UUID.randomUUID() + "\",\n";
                content += "\"name\": \"" + vox.getName().split("\\.")[0] + "\",\n";
                content += "\"size\": " + size + ",\n";
                content += "}";
                writer.write(content);
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
            Logger.getInstance().UpdateFile("MagicaRepli process done!\n\n");
        } catch (Exception e) {
            if (!Logger.getInstance().isInit) Logger.getInstance().init(String.valueOf(Paths.get("").toAbsolutePath()));
            Logger.getInstance().UpdateFile("------------ ERROR ------------\n" +e.toString()+"\n\n");
        }
    }

    static class Logger {
        public boolean isInit = false;
        String log = "";
        public static Logger instance = new Logger();

        public static Logger getInstance() {
            return instance;
        }

        File logFile = null;

        public Logger() {
        }

        public void init(String filepath) {
            Date date = new Date();
            logFile = new File(String.valueOf(Path.of(filepath, "MagicaRepli_" + date.getDay() + "-" + date.getMonth() + "-" + (Year.now().getValue()) + ".log")));
            if (logFile.exists()) {
                try {
                    for (String str :
                            Files.readAllLines(Path.of(String.valueOf(logFile)))) {
                        log += str + "\n";
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            UpdateFile("MagicaRepli started at " + new Date().getHours() + ":" + new Date().getMinutes() + ":" + new Date().getSeconds() + "\n");
            isInit = true;
        }

        public void UpdateFile(String data) {
            log += data;
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
                writer.write(log);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void log(String data) {
            String line = "[" + new Date().getHours() + ":" + new Date().getMinutes() + ":" + new Date().getSeconds() + "] " + data + "\n";
            UpdateFile(line);
            System.out.println(line);
        }
    }

    static class Voxel {
        int x, y, z, color;

        public Voxel(int x, int y, int z, int color) {
            this.x = x;
            this.y = z;
            this.z = y;
            this.color = color;
        }
    }
}

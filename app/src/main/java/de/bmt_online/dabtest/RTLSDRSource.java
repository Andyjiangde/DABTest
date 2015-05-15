package de.bmt_online.dabtest;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by Arbeit on 27.04.15.
 */
public class RTLSDRSource {
    public enum TunerType {
        E4000,
        FC0012,
        FC0013,
        FC2580,
        R820T,
        R828D;
    }

    public enum Commands {
        Frequency(0x01),
        SampleRate(0x02),
        GainMode(0x03),
        FreqCorr(0x05),
        IFGain(0x06),
        AGCMode(0x08);

        private final byte commandValue;

        Commands(
                final int commandValue) {
            this.commandValue =
                    (byte) commandValue;
        }
    }

    private final TunerType tunerType;
    private final SendCommandThread sendCommandThread;
    private final ReceiveDataThread receiveDataThread;

    public String message;

    public RTLSDRSource(
            final String rtlTCPAddress,
            final int rtlTCPPort) throws SocketException, UnknownHostException, IOException {
        Socket rtlTCPSocket =
                null;

        InputStream rtlTCPInputStream =
                null;
        OutputStream rtlTCPOutputStream =
                null;

        TunerType tunerType =
                null;

        try {
            rtlTCPSocket =
                    new Socket(
                            rtlTCPAddress,
                            rtlTCPPort);

            rtlTCPInputStream =
                    rtlTCPSocket.getInputStream();
            rtlTCPOutputStream =
                    rtlTCPSocket.getOutputStream();

            final byte[] buffer =
                    new byte[4];

            readIntoBuffer(
                    rtlTCPInputStream,
                    buffer);

            if (buffer[0] != 'R' ||
                    buffer[1] != 'T' ||
                    buffer[2] != 'L' ||
                    buffer[3] != '0') {
                throw new IOException(
                        "Missing protocol identifier 'RTL0'");
            }

            readIntoBuffer(
                    rtlTCPInputStream,
                    buffer);

            final long tunerTypeValue =
                    readUnsignedInteger(
                            rtlTCPInputStream);

//TODO mapping value <-> enum in enum mitdefinieren!!!
            switch ((int) tunerTypeValue) {
                case 1 :
                    tunerType =
                            TunerType.E4000;
                    break;

                case 2 :
                    tunerType =
                            TunerType.FC0012;
                    break;

                case 3 :
                    tunerType =
                            TunerType.FC0013;
                    break;

                case 4 :
                    tunerType =
                            TunerType.FC2580;
                    break;

                case 5 :
                    tunerType =
                            TunerType.R820T;
                    break;

                case 6 :
                    tunerType =
                            TunerType.R828D;
                    break;
            }

            if (tunerType == null) {
                throw new IOException(
                        "Could not detect tuner type!");
            }

            final long gainCount =
                    readUnsignedInteger(
                            rtlTCPInputStream);

//TODO fuer was ist gainCount gut???
        } catch (SocketException e) {
            if (rtlTCPSocket != null) {
                try {
                    rtlTCPSocket.close();
                } catch (SocketException ex) {
                    ex.printStackTrace();

                }
            }
        } catch (UnknownHostException e) {
            if (rtlTCPSocket != null) {
                try {
                    rtlTCPSocket.close();
                } catch (SocketException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (IOException e) {
            if (rtlTCPSocket != null) {
                try {
                    rtlTCPSocket.close();
                } catch (SocketException ex) {
                    ex.printStackTrace();

                }
            }
        }

        this.tunerType =
                tunerType;

        sendCommandThread =
                new SendCommandThread(
                        rtlTCPOutputStream);

        receiveDataThread =
                new ReceiveDataThread(
                        rtlTCPInputStream);
    }




    public void sendCommand(
            final Commands command,
            final int argument) {
        sendCommandThread.sendCommand(
                command,
                argument);
    }

    public void sendCommand(
            final Commands command,
            final short argument1,
            final short argument2) {
        sendCommandThread.sendCommand(
                command,
                argument1,
                argument2);
    }

    public byte[] getData() {
        return receiveDataThread.getData();
    }




    private final class ReceiveDataThread extends Thread {
        private final InputStream rtlTCPInputStream;
        private final ArrayList<byte[]> datas;

        ReceiveDataThread(
                final InputStream rtlTCPInputStream) {
            super("RTL-TCP-Receive-Data-Thread");

            this.rtlTCPInputStream =
                    rtlTCPInputStream;

            datas =
                    new ArrayList<byte[]>();

            start();
        }

        @Override
        public void run() {
            while (true) {
                final byte[] buffer =
                        new byte[16384];

                try {
                    readIntoBuffer(
                            rtlTCPInputStream,
                            buffer);
                } catch (IOException e) {
//TODO implement
                    e.printStackTrace();
                }

                synchronized (datas) {
                    datas.add(buffer);

                    datas.notifyAll();
                }
            }
        }

        byte[] getData() {
            synchronized (datas) {
                while (datas.size() == 0) {
                    try {
                        datas.wait();
                    } catch (InterruptedException e) {
//TODO implement
                        e.printStackTrace();

                    }
                }

                return datas.remove(0);
            }
        }

        void dispose() {
//TODO implement
        }
    }




    private final class SendCommandThread extends Thread {
        private final OutputStream rtlTCPOutputStream;
        private final ArrayList<byte[]> commands;

        SendCommandThread(
                final OutputStream rtlTCPOutputStream) {
            super("RTL-TCP-Send-Command-Thread");

            this.rtlTCPOutputStream =
                rtlTCPOutputStream;

            commands =
                    new ArrayList<>();

            start();
        }

        @Override
        public void run() {
            try {
                while (true) {
                    final byte[] command;

                    synchronized (commands) {
                        while (commands.size() == 0) {
                            commands.wait();
                        }

                        command =
                                commands.remove(
                                        0);
                    }

                    try {
                        rtlTCPOutputStream.write(
                                command);
                    } catch (IOException e) {
//TODO dispose?
                        e.printStackTrace();

                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        void sendCommand(
                final Commands command,
                final int argument) {
            final byte[] commandBytes =
                    new byte[5];

            commandBytes[0] =
                    command.commandValue;
            commandBytes[1] =
                    (byte) ((argument & 0xff000000) >> 24);
            commandBytes[2] =
                    (byte) ((argument & 0x00ff0000) >> 16);
            commandBytes[3] =
                    (byte) ((argument & 0x0000ff00) >>  8);
            commandBytes[4] =
                    (byte) ((argument & 0x000000ff) >>  0);

            synchronized (commands) {
                commands.add(
                        commandBytes);

                commands.notifyAll();
            }
        }

        void sendCommand(
                final Commands command,
                final short argument1,
                final short argument2) {
            final byte[] commandBytes =
                    new byte[5];

            commandBytes[0] =
                    command.commandValue;
            commandBytes[1] =
                    (byte) ((argument1 & 0xff00) >> 8);
            commandBytes[2] =
                    (byte) ((argument1 & 0x00ff) >> 0);
            commandBytes[3] =
                    (byte) ((argument2 & 0xff00) >> 8);
            commandBytes[4] =
                    (byte) ((argument2 & 0x00ff) >> 0);

            synchronized (commands) {
                commands.add(
                        commandBytes);

                commands.notifyAll();
            }
        }

        void dispose() {
//TODO implement
        }
    }






    private final static long readUnsignedInteger(
            final InputStream inputStream) throws IOException {
        final byte[] buffer =
                new byte[4];

        readIntoBuffer(
                inputStream,
                buffer);

        return ((buffer[0] & 0xff) << 24) |
               ((buffer[1] & 0xff) << 16) |
               ((buffer[2] & 0xff) <<  8) |
                (buffer[3] & 0xff);
    }

    private final static void readIntoBuffer(
            final InputStream inputStream,
            final byte[] buffer) throws IOException {
        int alreadyRead =
                0;
        int read;

        while ((read = inputStream.read(buffer,alreadyRead,buffer.length-alreadyRead)) != -1) {
            alreadyRead +=
                    read;

            if (alreadyRead == buffer.length) {
                break;
            }
        }

        if (alreadyRead < buffer.length) {
            throw new IOException(
                    "Not enough bytes to read!");
        }
    }



}

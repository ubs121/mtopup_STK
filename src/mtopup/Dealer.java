package mtopup;

import javacard.framework.*;
import sim.toolkit.*;
import sim.access.*;

/**
 * @author UB121
 */
public class Dealer extends Applet implements ToolkitInterface, ToolkitConstants {

    public Dealer() {
        data = JCSystem.makeTransientByteArray((short) 161, JCSystem.CLEAR_ON_RESET);
        info = JCSystem.makeTransientByteArray((short) 40, JCSystem.CLEAR_ON_RESET);
        ToolkitRegistry reg = ToolkitRegistry.getEntry();
        id = reg.initMenuEntry(title, (short)0, (short) title.length, 
                PRO_CMD_SELECT_ITEM, false, (byte)0, (short)0);
    }

    public static void install(byte[] bArray, short bix, byte bLength) {
        (new Dealer()).register();
    }

    public void processToolkit(byte event) {
        EnvelopeHandler envHdlr;
        envHdlr = EnvelopeHandler.getTheHandler();

        switch (event) {
            case EVENT_MENU_SELECTION:
                if (envHdlr.getItemIdentifier() == id) {
                    // check USSD support
                    if (MEProfile.check((byte)27) == true)
                        service();
                    else
                        Display(DCS_8_BIT_DATA, msgUssdNA, (short) 0, (short) msgUssdNA.length);
                }
                break;
        }
    }

    public void service() throws ToolkitException {
        byte s = (byte)'M'; // main menu is default
        byte END = (byte)0x0;
        
        // Main loop
        do {
            switch (s) {
                case 'M':
                    // Mobile Dealer
                    data[0] = (byte)0; /* set the data length to zero */
                    if (ShowMenu(title, home) == 0) {
                        ProactiveResponseHandler prh = ProactiveResponseHandler.getTheHandler();
                        s = prh.getItemIdentifier();
                    }
                    else
                        s = END;
                    break;
                case (byte)1: /* Ceneglex *142*MSISDN*Amount*MPIN# */ 
                    // concatenate *142
                    data[0] = 4; // length
                    data[1] = '*';
                    data[2] = '1';
                    data[3] = '4';
                    data[4] = '2';
                    
                    // *MSISDN
                    if (GetInput((byte)0, txtIsdn, (short)8, (short)8) == 0) {
                        info[0] =  (byte)(Util.arrayCopy(txtConfirm, (short)0, 
                                info, (short)1, (short)txtConfirm.length)-1);
                        Append(info, (byte)'\n');
                        Append(info);
                        
                        // *Amount
                        if (GetInput((byte)0, txtAmount, (short)1, (short)8) == 0) {
                            Append(info, (byte)'<');
                            Append(info);
                            
                            if (Display(DCS_8_BIT_DATA, info, (short)1, (short)info[0]) == 0) {
                                
                                // *MPIN
                                if (GetInput((byte)4, txtPin, (short)1, (short)5) == 0) {
                                    Append(data, (byte)'#');
                                    
                                    // send data
                                    PackAndSend();
                                }
                            }
                        }
                    }
            
                    s = 'M';
                    break;
                case (byte)2: /* Uldegdel shalgax *140*1*MPIN# */
                    // concatenate *140*1
                    data[0] = 6; // length
                    data[1] = '*';
                    data[2] = '1';
                    data[3] = '4';
                    data[4] = '0';
                    data[5] = '*';
                    data[6] = '1';
                    
                    // *MPIN
                    if (GetInput((byte)4, txtPin, (short)1, (short)5) == 0) {
                        Append(data, (byte)'#');
                        
                        // send data
                        PackAndSend();
                    }
                    s = 'M';
                    break;
                case (byte)3: /* Suuliin guilgee *140*2*MPIN#  */
                    // concatenate *140*2
                    data[0] = 6; // length
                    data[1] = '*';
                    data[2] = '1';
                    data[3] = '4';
                    data[4] = '0';
                    data[5] = '*';
                    data[6] = '2';
                    
                    // *MPIN
                    if (GetInput((byte)4, txtPin, (short)1, (short)5) == 0) {
                        Append(data, (byte)'#');
                        
                        // send data
                        PackAndSend();
                    }
                    s = 'M';
                    break;
                case (byte)4: /* Nuuc dugaar solix *147*OldMPIN*NewMPIN# */
                    // concatenate *147
                    data[0] = 4; // length
                    data[1] = '*';
                    data[2] = '1';
                    data[3] = '4';
                    data[4] = '7';
                    
                    // *OldMPIN
                    if (GetInput((byte)4, txtPin, (short)1, (short)5) == 0) {
                        // *NewMPIN
                        if (GetInput((byte)4, txtNewPin, (short)1, (short)5) == 0) {
                            Append(data, (byte)'#');
                            
                            // send data
                            PackAndSend();
                        }
                    }
                    s = 'M';
                    break;
                default:
                    s = END;
                    break;
            }
        } while (s != END);
    }

    public void process(APDU toProcess) {
    }
    
    // Append 1 byte
    private void Append(byte[] array, byte c) {
        array[++array[0]] = c;
    }
    
    // Append from input
    private void Append(byte[] array) {
        ProactiveResponseHandler prh = ProactiveResponseHandler.getTheHandler();
        array[0] = (byte)(prh.copyTextString(array, (short)(array[0]+1)) - 1);
    }
    
     /**
     *  Parameters
     *      title - Menu title
     *      items - Menu items
     * */
    private byte ShowMenu(byte[] title, Object[] items) {
        ProactiveHandler ph = ProactiveHandler.getTheHandler();
    
        ph.init(PRO_CMD_SELECT_ITEM, (byte)0, DEV_ID_ME);
        ph.appendTLV((byte) (TAG_ALPHA_IDENTIFIER | TAG_SET_CR), title, 
                (short) 0, (short) title.length);
        for (byte ix = 0; ix < items.length; ix++) {
            ph.appendTLV((byte)(TAG_ITEM | TAG_SET_CR), (byte)(ix+1), (byte[])items[ix], 
                    (short)0, (short)((byte[])items[ix]).length);
        }
        
        return ph.send();
    }
   
     /**
      * Description
      *     Gets the input from user
      * Parameters
      *     type - 0=Digit, 1=Text, 4=Password
      *     title - Input title
      *     min - min length of response
      *     max - max length of response
      * */
    private byte GetInput(byte type, byte[] title, short min, short max) {
        ProactiveHandler ph = ProactiveHandler.getTheHandler();
        ph.initGetInput(type, DCS_8_BIT_DATA, title, 
                (short)0, (short)title.length, min, max);

        // Append
        if (ph.send() == 0) {
            Append(data, (byte)'*');
            Append(data);
            
            return 0;
        }
        
        return 1;
    }
    
    /**
     *  Description
     *      Pack USSD data and send it
     * */
    private void PackAndSend() {
        /* Convert to 7 bit data */
        byte len = data[0];
        byte mask;
        byte d = 0; // Difference between position of the byte to be read and the position of the byte to be written
        short ix;
        short ix8 = 0; // Index by 8 step
        byte n = 0;
        
        while (n < len) {
            // Pack seven bytes at a time
            for (byte j = (byte) 0; j <= 7; j++) {
                ix = (short)(1 + ix8 + j); // Current index
                n++;
                // Read next byte and shift right 0 for byte 1, 1 for byte 2 etc.
                data[(short)(ix-d)] = (byte) (data[ix] >> j);
                // Get mask for lsb of next in byte to apply to msb of out byte
                if (n == len) {
                    mask = (byte) 0;
                    // Apply mask to out byte
                    data[(short)(ix-d)] |= mask;
                    break;
                } else {
                    mask = Mask((byte) (j+1), data[(short) (ix+1)]);
                    // Apply mask to out byte
                    data[(short)(ix-d)] |= mask;
                    if ((byte) (n % 8) == (byte) 0) {
                        d++;
                    }
                }
            }
            ix8 = (short) (ix8 + 8);
        }

        // If lengthCharacters = (8n - 1) pad with 0x1A (see GSM 03.38)
        if ((short) (len % 8) == (short)7) {
            data[(short) (len-d)] |= (byte) 0x1A; // CR << 1 = 0x1A
        }
        
        // Cut the tailing bytes
        if ((short)((short)(len * 7) % (short)8) == (short)0)
            data[0] = (byte)(len - d - 1);
        else
            data[0] = (byte)(len - d);
        
         // Check the data to be sent
        //Display(DCS_DEFAULT_ALPHABET, data, (short)1, (short)data[0]);
        
        /* Send USSD */
        ProactiveHandler ph = ProactiveHandler.getTheHandler();
        ph.init(PRO_CMD_SEND_USSD, (byte) 0, DEV_ID_NETWORK);
        ph.appendTLV(TAG_ALPHA_IDENTIFIER, msgTopUp, 
                (short)0, (short) msgTopUp.length);
        ph.appendTLV((byte)0x0A, (byte)0x0F, data, (short)1, (short)data[0]);
        
        if (ph.send() == 0) {
            ProactiveResponseHandler prh = ProactiveResponseHandler.getTheHandler();
            data[0] = (byte)(prh.findAndCopyValue(TAG_TEXT_STRING, data, (short)0)-1);
            /* Show the response from network */
            Display(DCS_DEFAULT_ALPHABET, data, (short)1, (short)data[0]);
        } else
            Display(DCS_8_BIT_DATA, msgSendErr, (short)0, (short)msgSendErr.length);
    }

    /**
     *  Parameters
     *      dcs - dat Coding Scheme (DCS_DEFAULT_ALPHABET = 00, DCS_8_BIT_dat = 04)
     *      msg - display message
     * */
    private byte Display(byte dcs, byte[] msg, short ix, short length) {
        // 0x00 - Display text automatically cleared after a delay
        // 0x80 - Display text cleared only after user action on mobile
        ProactiveHandler ph = ProactiveHandler.getTheHandler();
        ph.initDisplayText((byte) 0x80, dcs, msg, ix, length);
        return ph.send();
    }

    /**
     *  Description
     *      Get the mask for given val
     *  Parameters
     *      bits - bit count
     *      val - val
     * */
    private byte Mask(byte bits, byte val) {
        byte mask = (byte) 0;
        for (byte j = 0; j < bits; j++) {
            if (j == 0)
                mask = (byte)1;
            else
                mask = (byte)(1 + (byte)(mask * 2));
        }

        byte res = (byte) (mask & val);
        res <<= (byte) (8 - bits);

        return res;
    }
    
    // Globals
    private byte id;
    private byte[] data;
    private byte[] info;
   
    // Messages
    private final byte[] msgUssdNA = {'U','S','S','D',' ','d','e','m','j','i','x','g','u','i','!'};
    private final byte[] msgTopUp = {'T','o','p',' ','U','p','.','.','.'};
    private final byte[] msgSendErr = {'X','o','l','b','o','l','t','i','i','n',' ','a','l','d','a','a','!'};
    
    // Dealer menu
    private final byte[] title = {'D','e','a','l','e','r'};
    private final Object[] home = {
        new byte[] {'C','e','n','e','g','l','e','x'},
        new byte[] {'U','l','d','e','g','d','e','l',' ','s','h','a','l','g','a','x'},
        new byte[] {'S','u','u','l','i','i','n',' ','g','u','i','l','g','e','e'},
        new byte[] {'N','u','u','c',' ','k','o','d',' ','s','o','l','i','x'}
    };
                    
    // Input items
    private final byte[] txtIsdn = {'C','e','n','e','g','l','e','x',' ','d','u','g','a','a','r','?'};
    private final byte[] txtAmount = {'D','u','n','?'};
    private final byte[] txtPin = {'N','u','u','c',' ','k','o','d','?'};
    private final byte[] txtNewPin = {'S','h','i','n','e',' ','n','u','u','c',' ','k','o','d','?'};
    private final byte[] txtConfirm = {'I','l','g','e','e','x',' ','u','u','?'};
}

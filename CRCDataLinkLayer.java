// =============================================================================
/**
 *  A data link layer that uses start/stop tags and byte packing to frame the
 *  data, and that uses a single parity bit to perform error detection.
 *
 * @author Scott F. H. Kaplan -- http://www.cs.amherst.edu/~sfkaplan
 * @date 2008 March 03
 * @version %I% %G%
 **/
public class CRCDataLinkLayer extends DataLinkLayer {
// =============================================================================



    // =========================================================================
    /**
     * The constructor.  Make a new parity-checking data link layer.
     *
     * @param physicalLayer The physical layer through which this data link
     * layer should communicate.
     **/
    public CRCDataLinkLayer (PhysicalLayer physicalLayer) {

	// Initialize the layer.
	initialize(physicalLayer);

    } // ParityDataLinkLayer
    // =========================================================================



    // =========================================================================
    /**
     * Accept a buffer of data to send.  Send it as divided into multiple frames
     * of a fixed, maximum size.  Add a parity bit for error checking to each
     * frame.  Call the physical layer to actually send each frame.
     *
     * @param data An array of bytes to be framed and transmitted.
     **/
    public void send (byte[] data) {

	// Calculate the number of frames needed to transmit this data.
	int numberFrames = (int)Math.ceil((double)data.length / _maxFrameSize);

	// Construct each frame and send it.
	for (int frameNumber = 0; frameNumber < numberFrames; frameNumber++) {

	    int beginIndex = _maxFrameSize * frameNumber;
	    int endIndex = _maxFrameSize * (frameNumber + 1);
	    if (endIndex > data.length) {
		endIndex = data.length;
	    }
	    byte[] frame = constructFrame(data, beginIndex, endIndex);
	    physicalLayer.send(frame);

	}

    } // send (byte[] data)
    // =========================================================================



    // =========================================================================
    /**
     * Create a single frame to be transmitted.
     *
     * @param data The original buffer of data from which to extract a frame's
     *             worth.
     * @param begin The starting index from the original data buffer.
     * @param end The ending index from the original frame buffer.
     * @return A byte array that contains an entirely constructed frame.
     **/
    private byte[] constructFrame (byte[] data, int begin, int end) {

	// Allocate an array of bytes large enough to hold the largest possible
	// frame (tags and parity byte included).
	byte[] framedData = new byte[(_maxFrameSize * 4) + 3];

	// Begin with the start tag.
	int frameIndex = 0;
	framedData[frameIndex++] = _startTag;

	// Add each byte of original data.
	for (int dataIndex = begin; dataIndex < end; dataIndex++) {

	    // If the current data byte is itself a metadata tag, then preceed
	    // it with an escape tag.
	    byte currentByte = data[dataIndex];
	    if ((currentByte == _startTag) ||
		(currentByte == _stopTag) ||
		(currentByte == _escapeTag)) {

		framedData[frameIndex++] = _escapeTag;

	    }

	    // Add the data byte itself.
	    framedData[frameIndex++] = currentByte;

	}

	// Create the finalFrame, base on the frame data we just created and the CRC
    byte[] finalFrame = new byte[(_maxFrameSize * 4) + 3];
	finalFrame = calculateCRC(framedData, 0, frameIndex);
    //System.out.println("This is the CRCDDL");

	// End with a stop tag.
    /*
	framedData[frameIndex++] = _stopTag;

	// Copy the complete frame into a buffer of the exact desired
	// size.
	byte[] finalFrame = new byte[frameIndex];
	for (int i = 0; i < frameIndex; i++) {
	    finalFrame[i] = framedData[i];
        //System.out.print(finalFrame[i]);
	}
    
        //System.out.println();
    */
	return finalFrame;

    } // constructFrame (byte[] data, int begin, int end)
    // =========================================================================



    // =========================================================================
    /**
     * Calculate the parity of the sequence of bytes.
     *
     * @param data A buffer of bytes.
     * @param begin The starting index of the bytes to examine.
     * @param end The ending index of the bytes to examine.
     * @return The parity (0 or 1) for this group of bytes.
     **/
    
    //calculate the transmit frame by get the remainder than subtract it from the original bits
    private BitVector PolyDiv(BitVector one, BitVector two){
        int oneIndex = 0;
        BitVector carry = new BitVector();
        for (int i=0;i<two.length();i++){
            carry.setBit(i,one.getBit(i));
        }
        //printBit(carry);
        while (oneIndex+two.length()<=one.length()){
            carry.setBit(two.length()-1,one.getBit(oneIndex+two.length()-1));
            //printBit(carry);
            boolean token = carry.getBit(0);
            for (int i=1;i<two.length();i++){
                if (token==true)
                    carry.setBit(i-1,carry.getBit(i)^two.getBit(i));
                else
                    carry.setBit(i-1,carry.getBit(i)^false);
            }
            oneIndex++;
        }
        //printBit(carry);
        for (int i=0;i<carry.length()-1;i++){
            one.setBit(one.length()-i-1,one.getBit(one.length()-i-1)^carry.getBit(carry.length()-i-2));
        }
        return one;
    }
    
    //create the Generator bits
    private BitVector createG(){
        BitVector gfactor = new BitVector();
        gfactor.setBit(0,true);
        gfactor.setBit(16,true); // 15 is the maxlength of burst error in the burstyNoiseMedium
        return gfactor;
    }
    
    private BitVector createBit(String str){
        BitVector bits = new BitVector();
        for (int i=0;i<str.length();i++){
            if (str.charAt(i)=='1')
                bits.setBit(i,true);
            else
                bits.setBit(i,false);
        }
        return bits;
    }


    private void printBit(BitVector bits){
        int one;
        for (int i = 0; i < bits.length(); i++) {
            one = (bits.getBit(i) ? 1 : 0);
            System.out.print(one);
        }
        System.out.println();
    }

    
    private byte[] calculateCRC (byte[] data, int begin, int end) {
	// Create a bit vector from the bytes specified.
	BitVector bits = new BitVector(data, begin, end);
    byte[] finalFrame = new byte[(_maxFrameSize * 4) + 3];

	// Iterate over the bit vector, counting the bits whose value is 1.
	int ones = 0;
    BitVector gfactor = createG();
    int leng = bits.length();
    for (int i=0;i<gfactor.length()-1;i++){
        bits.setBit(leng+i,false);
    } //append gfactor level of 0 to bits
    
    BitVector finalBits = PolyDiv(bits,gfactor);
    byte[] stop = new byte[1];
    stop[0] = _stopTag;
    BitVector stopTagBit = new BitVector(stop,0,1);
    //printBit(stopTagBit);
    int lengfinal = finalBits.length();
    //System.out.print("s bits ");
    //printBit(finalBits);
    for (int i=0;i<stopTagBit.length();i++){
        finalBits.setBit(lengfinal+i,stopTagBit.getBit(i));
    }
    finalFrame = finalBits.toByteArray();
    //printBit(finalBits);
    /*
    int frameIndex=0;
    while (finalFrame[frameIndex]!=_stopTag||finalFrame[frameIndex-1]==_escapeTag) {
        System.out.print(finalFrame[frameIndex]);
        frameIndex++;
	}
    System.out.println();
	*/
    // Return the the whole frame which contain both the data and the CRC.
	return finalFrame;

    } // calculateParity (byte[] data, int begin, int end)
    // =========================================================================


    // =========================================================================
    /**
     * Determine whether the buffered data forms a complete frame.
     *
     * @return Whether a complete buffer has arrived.
     **/
    protected boolean receivedCompleteFrame () {

	// Any frame with less than two bytes cannot be complete, since even the
	// empty frame contains a start and a stop tag.
	if (bufferIndex < 2) {

	    return false;

	}

	// A frame is complete iff the byte received is an non-escaped stop tag.
	return ((incomingBuffer[bufferIndex - 1] == _stopTag) &&
		(incomingBuffer[bufferIndex - 2] != _escapeTag));

    } // receivedCompleteFrame
    // =========================================================================



    // =========================================================================
    /**
     *  Remove the framing metadata and return the original data.
     *
     * @return The data carried in this frame; <tt>null</tt> if the data was not
     *         successfully received.
     **/
    protected byte[] processFrame () {

	// Allocate sufficient space to hold the original data, which
	// does not need space for the start/stop tags.
	byte[] originalData = new byte[bufferIndex];

	// Check the start tag.
	int frameIndex = 0;
	if (incomingBuffer[frameIndex++] != _startTag) {

	    System.err.println("ParityDLL: Missing start tag!");
	    return null;

	}
    //System.out.println(frameIndex);
	// Loop through the frame, extracting the bytes.  Look ahead to find the
	// stop tag (making sure it is not escaped), because the byte before
	// that is the parity byte.
	int originalIndex = 0;
    frameIndex = 0;
	while ((incomingBuffer[frameIndex] != _stopTag) ||
	       (incomingBuffer[frameIndex-1] == _escapeTag)) {
        //System.out.println(frameIndex);
	    // Copy the original byte.
	    originalData[originalIndex] = incomingBuffer[frameIndex];
        originalIndex++;
        frameIndex++;
	}

	// Allocate a space that is only as large as the original
	// message and then copy the original data into it.
    
    BitVector gfactor = createG();
    BitVector frameBits = new BitVector(originalData,0,originalIndex);
    BitVector divResult = new BitVector();
    for (int i=0;i<frameBits.length();i++){
            divResult.setBit(i,frameBits.getBit(i));
    }
    divResult = PolyDiv(divResult,gfactor);
    Boolean check = false;
    for (int i=0;i<frameBits.length();i++){
        if (frameBits.getBit(i)^divResult.getBit(i)==true){
            check = true;
            System.out.println("CRC checked error found");
            return null;
        }
    }
    System.out.println("No error");


    byte[] finalData = new byte[originalIndex-2];
    int finalIndex = 0;
    for (int i = 0; i < originalIndex-3; i++) {
        if (originalData[i+1]!=_escapeTag){
            finalData[finalIndex] = originalData[i+1];
            finalIndex++;
        }
    }
    
    byte[] finalfinalData = new byte[finalIndex];
    for (int i = 0; i < finalIndex; i++) {
        finalfinalData[i]=finalData[i];
    }
    
	// Calculate the parity of the extracted data and compare it to the
	// received parity bit.  If there's a mismatch, return null.
	/*
    byte parity = calculateCRC(originalData, 0, originalIndex);
	if (parity != incomingBuffer[frameIndex]) {

	    System.err.print("ParityDLL message: ");
	    for (int i = 0; i < finalData.length; i++) {
		System.err.print((char)finalData[i]);
	    }
	    System.err.println(" <= Parity mismatch!");
	    finalData = null;

	}
*/
	return finalfinalData;

    } // processFrame
    // =========================================================================



    // =========================================================================
    // DATA MEMBERS

    /**
     * The tag that marks the beginning of a frame.
     **/
    final byte _startTag = (byte)'{';

    /**
     * The tag that marks the end of a frame.
     **/
    final byte _stopTag = (byte)'}';

    /**
     * The tag that marks the following byte as data (and not metadata).
     **/
    final byte _escapeTag = (byte)'\\';

    /**
     * The maximum number of data (not metadata) bytes in a frame.
     **/
    final int _maxFrameSize = 8;
    // =========================================================================



// =============================================================================
} // class ParityDataLinkLayer
// =============================================================================

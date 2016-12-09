import java.util.PriorityQueue;

/**
 *	Interface that all compression suites must implement. That is they must be
 *	able to compress a file and also reverse/decompress that process.
 * 
 *	@author Brian Lavallee
 *	@since 5 November 2015
 *  @author Owen Atrachan
 *  @since December 1, 2016
 */
public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); // or 256
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;
	public static final int HUFF_COUNTS = HUFF_NUMBER | 2;

	public enum Header{TREE_HEADER, COUNT_HEADER};
	public Header myHeader = Header.TREE_HEADER;
	
	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){
		int [] counts = readForCounts(in);
		TreeNode root = makeTreeFromCounts(counts);
		String [] codings = new String [257];
		String path = "";
		codings = makeCodingsFromTree(root, path, codings);
		writeHeader(out, root);
		in.reset();
		writeCompressedBits(in, out, codings);
	}
	
	public int [] readForCounts(BitInputStream in){
		int [] ret = new int [256];
		while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1)
				break;
			ret[val]++;
		}
		return ret;
	}
	
	//code given in assignment
	public TreeNode makeTreeFromCounts(int [] counts){
		PriorityQueue<TreeNode> pq = new PriorityQueue<>();
		
		for (int i=0; i<256; i++){//made 256 to make room for the PSEUDO 
			if (counts[i]!=0)
			pq.add(new TreeNode(i, counts[i], null, null));
		}
			pq.add(new TreeNode(PSEUDO_EOF, 1, null, null));
			
			while (pq.size() > 1) {
			    TreeNode left = pq.remove();
			    TreeNode right = pq.remove();
			    TreeNode t = new TreeNode(-1, left.myWeight + right.myWeight, left,right);
			    pq.add(t);
			}
			TreeNode root = pq.remove();
			return root;
	}
	
	//works
	public String [] makeCodingsFromTree(TreeNode root, String path, String [] codings){
		if (root==null)
			return codings;
		
		if (root.myLeft==null && root.myRight==null){
     	   codings[root.myValue]=path;
     	   return codings;
		}
		makeCodingsFromTree(root.myLeft, path+"0", codings);
		makeCodingsFromTree(root.myRight, path+"1", codings);
		return codings;
	}
	
	public void writeHeader(BitOutputStream out, TreeNode root){
		out.writeBits(BITS_PER_INT, HUFF_NUMBER);
		writeTree(out, root);
	}
	
	public void writeTree(BitOutputStream out, TreeNode root){
		if (root==null)
			return;
		if (root.myLeft==null && root.myRight==null){
			out.writeBits(1, 1);
			out.writeBits(9, root.myValue);
		}else{
			out.writeBits(1, 0);
			writeTree(out, root.myLeft);
			writeTree(out, root.myRight); 
		}
	}
	
	public void writeCompressedBits(BitInputStream in, BitOutputStream out, String[] codings){
		while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1)
				break;
			String encode = codings[val];
			out.writeBits(encode.length(), Integer.parseInt(encode,2));
		}
		String encode = codings[256];
		out.writeBits(encode.length(), Integer.parseInt(encode,2));
	}

	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){
		int id = in.readBits(BITS_PER_INT);
	    if (id!=HUFF_NUMBER && id!=HUFF_TREE)
	    	throw new HuffException("invalid bits per int");
	   
	    TreeNode root = readTreeHeader(in);
	    readCompressedBits(root,in,out);
	}

	public TreeNode readTreeHeader(BitInputStream in){
		int bit = in.readBits(1);
			if (bit==1){
				int nodeVal = in.readBits(9);
				return new TreeNode(nodeVal, 0, null, null);
			}
			return new TreeNode(0,0, readTreeHeader(in), readTreeHeader(in));
	}
	
	public void readCompressedBits(TreeNode root, BitInputStream in, BitOutputStream out){
		TreeNode current = root;
		
		while (true){
			int bits = in.readBits(1);
				if (bits==0){
					current=current.myLeft;
				}else if (bits==1){
					current = current.myRight;
				}
					if (current.myLeft==null && current.myRight==null){ //reached a leaf
						if(current.myValue==PSEUDO_EOF){
							break;
						}else{
							out.writeBits(BITS_PER_WORD, current.myValue);
							current = root;
						}
					}
			}
		}
	
	
	public void setHeader(Header header) {
        myHeader = header;
        System.out.println("header set to "+myHeader);
    }
}
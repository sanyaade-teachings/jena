/*
 * Created on 22-Nov-2003
 * 
 * 
 * Perf notes:
 * 
 * Running WG tests
 *  - no checking whatsoever (just parsing and imports)
 *    12.5 sec
 *  - checking with qrefine 18.5 (down to 17.3)
 *  - checking with old refineTriple 28
 *   (down to 20 with reduced grammar)
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package com.hp.hpl.jena.ontology.tidy;

import com.hp.hpl.jena.shared.*;
import java.util.*;
import java.io.*;
/**
 * @author jjc
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class Compiler implements Constants {

	final private String SAVEFILE = "tmp/huge.ser";
	static private long lookup[][];

	boolean validate() {
		if (prop(qrefine(14, 80, 89)) != 80) {
			System.err.println("gggg");
			return false;
		}
		Iterator it = huge.keySet().iterator();
		while (it.hasNext()) {
			Long l = (Long) it.next();
			long ll = l.longValue();
			int s = subject(ll);
			int p = prop(ll);
			int o = object(ll);
			long lk = qrefine(s, p, o);
			int ik = LookupTable.qrefine(s, p, o);
			if (subject(lk) != LookupTable.subject(ik))
				return false;

			if (prop(lk) != LookupTable.prop(ik))
				return false;
			if (object(lk) != LookupTable.object(ik))
				return false;
			if (allActions(lk) != LookupTable.allActions(ik))
				return false;
		}
		return true;
	}
	private void saveResults() {
		int key[] = new int[huge.size()];
		int value[] = new int[huge.size()];
		byte action[] = new byte[huge.size()];
		if (1 << WW <= possible.size())
			throw new BrokenException("Compiler failure: WW is not big enough");
		Iterator it = huge.entrySet().iterator();
		int i = 0;
		while (it.hasNext()) {
			Map.Entry ent = (Map.Entry) it.next();
			long k = ((Long) ent.getKey()).longValue();
			long v = ((Long) ent.getValue()).longValue();
			short spo[] = expand(k);
			key[i] = (spo[0] << (WW * 2)) | (spo[1] << WW) | spo[2];
			spo = expand(v);
			value[i] = (spo[0] << (WW * 2)) | (spo[1] << WW) | spo[2];
			action[i] = (byte) spo[3];
			if (i > 0 && key[i] <= key[i - 1])
				throw new BrokenException("Sort error");
			i++;
		}

		try {
			FileOutputStream ostream = new FileOutputStream(DATAFILE);
			ObjectOutputStream p = new ObjectOutputStream(ostream);
			p.writeObject(key);
			p.writeObject(value);
			p.writeObject(action);
			p.writeObject(CategorySet.unsorted);
			p.flush();
			ostream.close();
		} catch (IOException e) {
			throw new BrokenException(e);
		}
	}

	private void initLookup() {
		lookup = new long[huge.size()][2];
		Iterator it = huge.entrySet().iterator();
		int i = 0;
		while (it.hasNext()) {
			Map.Entry ent = (Map.Entry) it.next();
			lookup[i][0] = ((Long) ent.getKey()).longValue();
			lookup[i][1] = ((Long) ent.getValue()).longValue();
			i++;
		}
	}

	static private Comparator comp = new Comparator() {

		public int compare(Object o1, Object o2) {

			long rslt = ((long[]) o1)[0] - ((long[]) o2)[0];
			if (rslt < 0)
				return -1;
			if (rslt > 0)
				return 1;
			return 0;
		};
	};
	static {
		Compiler c = new Compiler();
		c.restore();
		c.initLookup();
		for (int i = 1; i < lookup.length; i++)
			if (comp.compare(lookup[i - 1], lookup[i]) >= 0) {
				throw new BrokenException("lookup init");
			}
	}
	static long qrefine(int s, int p, int o) {
		long key[] = { SubCategorize.toLong(s, p, o), 0 };
		int rslt = Arrays.binarySearch(lookup, key, comp);
		if (rslt < 0)
			return Failure;
		else
			return lookup[rslt][1];
	}

	private boolean restore() {
		try {
			FileInputStream istream = new FileInputStream(SAVEFILE);
			ObjectInputStream p = new ObjectInputStream(istream);
			huge = (SortedMap) p.readObject();
			possible = (SortedSet) p.readObject();
			Vector v = (Vector) p.readObject();
			Iterator it = v.iterator();
			while (it.hasNext()) {
				((CategorySet) it.next()).restore();
			}
			istream.close();
		} catch (FileNotFoundException ee) {
			return false;
		} catch (IOException e) {
			throw new BrokenException(e);
		} catch (ClassNotFoundException e) {
			throw new BrokenException(e);
		}
		return true;
	}
	private void save() {
		try {
			FileOutputStream ostream = new FileOutputStream(SAVEFILE);
			ObjectOutputStream p = new ObjectOutputStream(ostream);
			p.writeObject(huge);
			p.writeObject(possible);
			p.writeObject(CategorySet.unsorted);
			p.flush();
			ostream.close();
		} catch (IOException e) {
			throw new BrokenException(e);
		}
	}
	SortedSet possible = new TreeSet();
	SortedMap huge = new TreeMap();
	SortedMap moreThan = new TreeMap();
	SortedMap lessThan = new TreeMap();
	SortedMap comparablePairs = new TreeMap();
	Map morePossible = new HashMap();
	Map oldMorePossible = null;
	void add(int i, int was) {
		Integer ii = new Integer(i);
		if ((!possible.contains(ii)) && !morePossible.containsKey(ii))
			morePossible.put(ii, new Integer(was));
	}
	void add(int i) {
		add(i, -1);
	}
	Long toLong(int s2, int p2, int o2) {
		return new Long(
			((((long) s2) << (2 * W))
				| (((long) p2) << (1 * W))
				| (((long) o2) << (0 * W))));
	}
	void spo(int s, int p, int o) {
		//long r = SubCategorize.refineTriple(s, p, o);
		long r1;
		if (oldMorePossible != null) {
			boolean sOld, pOld, oOld;
			int s1, p1, o1;
			Integer is0 = (Integer) oldMorePossible.get(new Integer(s));
			if (is0 != null) {
				sOld = true;
				s1 = is0.intValue();
			} else {
				sOld = false;
				s1 = s;
			}
			Integer ip0 = (Integer) oldMorePossible.get(new Integer(p));
			if (ip0 != null) {
				pOld = true;
				p1 = ip0.intValue();
			} else {
				pOld = false;
				p1 = p;
			}
			Integer io0 = (Integer) oldMorePossible.get(new Integer(o));
			if (io0 != null) {
				oOld = true;
				o1 = io0.intValue();
			} else {
				oOld = false;
				o1 = o;
			}
			Long rold = toLong(s1, p1, o1);
			Long old = (Long) huge.get(rold);
			if (old == null) {
				//				if (Grammar.Failure != r) {
				//					if (Grammar.Failure != r)
				//						System.err.println("E2");
				//					System.err.println(
				//						CategorySet.toString(toLong(s1, p1, o1)));
				//					System.err.println(CategorySet.toString(toLong(s, p, o)));
				//					System.err.println(CategorySet.toString(r));
				//				}
				return;
			}
			long r0 = old.longValue();
			//			if ( SubCategorize.spo(r0)==rold.longValue()) 
			//			  r1 = r0;
			//			else
			r1 =
				SubCategorize.refineTriple(
					sOld ? s : subject(r0),
					pOld ? p : prop(r0),
					oOld ? o : object(r0));
			//			if (r1 != r) {
			//				System.err.println(CategorySet.toString(toLong(s0, p0, o0)));
			//				System.err.println(CategorySet.toString(toLong(s, p, o)));
			//				System.err.println(CategorySet.toString(r0));
			//				System.err.println(CategorySet.toString(r));
			//				System.err.println(CategorySet.toString(r1));
			//
			//			}

		} else {
			r1 = SubCategorize.refineTriple(s, p, o);
			;
		}

		if (r1 != Failure //	 && !SubCategorize.dl(r)
		) {
			huge.put(toLong(s, p, o), new Long(r1));
			add(subject(r1), s);
			add(prop(r1), p);
			add(object(r1), o);
		}

	}
	void add(int a, int b, int c) {
		spo(a, b, c);
		if (a != b)
			spo(b, a, c);
		if (a != c)
			spo(b, c, a);
		if (b != c) {
			spo(a, c, b);
			if (a != c)
				spo(c, a, b);
			if (a != b)
				spo(c, b, a);
		}
	}
	void initPossible() {
		add(Grammar.blank);
		add(Grammar.classOnly);
		add(Grammar.propertyOnly);
		add(Grammar.literal);
		add(Grammar.liteInteger);
		add(Grammar.dlInteger);
		add(Grammar.userTypedLiteral);
		add(Grammar.userID);
		Set ignore = new HashSet();
		ignore.add(new Integer(0));
		Iterator it = morePossible.keySet().iterator();
		while (it.hasNext()) {
			int c = ((Integer) it.next()).intValue();
			int cat[] = CategorySet.getSet(c);
			for (int i = 0; i < cat.length; i++)
				ignore.add(new Integer(cat[i]));
		}
		for (int i = 0; i < CategorySet.unsorted.size(); i++)
			if (!ignore.contains(new Integer(i)))
				add(i);
	}
	static long start = System.currentTimeMillis();
	void compute() {
		initPossible();
		while (!morePossible.isEmpty()) {
			possible.addAll(morePossible.keySet());
			Iterator it1 = morePossible.entrySet().iterator();
			morePossible = new HashMap();
			int c = 0;
			while (it1.hasNext()) {
				if (c++ % 20 == 0)
					log("G", c);
				Map.Entry ent = (Map.Entry) it1.next();
				int n1 = ((Integer) ent.getKey()).intValue();
				//	int old1 = ((Integer) ent.getValue()).intValue();
				Iterator it2 = possible.iterator();
				while (it2.hasNext()) {
					Integer ni2 = (Integer) it2.next();
					Iterator it3 = possible.tailSet(ni2).iterator();
					while (it3.hasNext())
						add(
							n1,
							ni2.intValue(),
							((Integer) it3.next()).intValue());
				}
			}
			oldMorePossible = morePossible;
		}
		System.err.println("Saving");
		save();
		log("GX", 0);
	}
	private void go2() {
		if (restore())
			System.err.println("Restore successful");
		else {
			System.err.println("Restore unsuccessful: recomputing");
			compute();
		}
		System.err.println(validate() ? "Good" : "Bad");
	}
	private void go() {
		if (restore())
			System.err.println("Restore successful");
		else {
			System.err.println("Restore unsuccessful: recomputing");
			compute();
		}
		log("GX", 0);
		makeLessThan();
		//	roy();
		//makeMeet();
		//makeJoin();
		//	pairs();
		//	System.err.println("XXX=" + evalPairs());
		findUseless();
		System.err.println("Saving results");
		saveResults();
		System.err.println("Saving data");
		save();
	}

	static final int[] intersection(int a[], int b[]) {
		int rslt0[] = new int[a.length];
		int k = 0;
		for (int i = 0; i < a.length; i++)
			if (Q.member(a[i], b))
				rslt0[k++] = a[i];
		int rslt1[] = new int[k];
		System.arraycopy(rslt0, 0, rslt1, 0, k);
		return rslt1;
	}
	private Integer compare(int i, int j) {
		return compare(new Pair(i, j));
	}
	private Integer compare(Pair p) {
		return (Integer) comparablePairs.get(p);
	}
	private Integer compare(Integer i, Integer j) {
		return compare(i.intValue(), j.intValue());
	}

	/*
	 * from *>* to;  where *>* is the partial order
	 * defined by category sets.   
	 */
	private void makeLessThan(int from, int to) {
		//if (from == to)
		//		return;
		Pair p = new Pair(from, to);
		if (!comparablePairs.containsKey(p)) {
			comparablePairs.put(p, new Integer(to));
			Set s = (Set) lessThan.get(new Integer(from));
			if (s == null) {
				s = new HashSet();
				lessThan.put(new Integer(from), s);
			}
			s.add(new Integer(to));
			s = (Set) moreThan.get(new Integer(to));
			if (s == null) {
				s = new HashSet();
				moreThan.put(new Integer(to), s);
			}
			s.add(new Integer(from));
		}
	}
	/*
	void lessThan() {
		int i = 0;
		Iterator it = huge.entrySet().iterator();
		Map.Entry ent;
		while (it.hasNext()) {
			if (i++ % 200000 == 0)
				log("LT", i);
			ent = (Map.Entry) it.next();
			long k = ((Long) ent.getKey()).longValue();
			long v = ((Long) ent.getValue()).longValue();
			lessThan(SubCategorize.subject(k), SubCategorize.subject(v));
			lessThan(SubCategorize.prop(k), SubCategorize.prop(v));
			lessThan(SubCategorize.object(k), SubCategorize.object(v));
		}
	}
	*/
	private void makeLessThan() {
		Iterator it1 = possible.iterator();
		int c = 0;
		while (it1.hasNext()) {
			Integer ni1 = (Integer) it1.next();
			int i1 = ni1.intValue();
			int c1[] = CategorySet.getSet(i1);
			Iterator it2 = possible.iterator();
			//		it2.next();
			while (it2.hasNext()) {
				if (c++ % 200000 == 0)
					log("LT", c);
				Integer ni2 = (Integer) it2.next();
				int i2 = ni2.intValue();
				int c2[] = CategorySet.getSet(i2);

				if (isLessThan(c1, c2))
					makeLessThan(i2, i1);
			}

		}
		log("LTX", 0);

	}
	static private boolean isLessThan(int small[], int big[]) {
		if (small.length > big.length)
			return false;
		int i = 0;
		int j = 0;
		while (true) {
			if (i == small.length)
				return true;
			if (j == big.length)
				return false;
			if (small[i] == big[j]) {
				i++;
				j++;
			} else {
				if (small[i] < big[j])
					return false;
				j++;
			}
		}
	}
	private void log(String m, int c) {
		System
			.err
			.println(
				m
				+ ": "
				+ c
				+ " "
				+ morePossible.size()
				+ "/"
				+ possible.size()
				+ "/"
				+ huge.size()
				+ "/"
				+ comparablePairs.size()
				+ "/"
		/*
		+ meet.size()
		+ "/"
		+ join.size()
		+ "{"
		+ pairs[0].size()
		+ ","
		+ pairs[1].size()
		+ ","
		+ pairs[2].size()
		+ "}" */
		+ (System.currentTimeMillis() - start) / 1000);
	}
	private Iterator supers(int x) {
		return ((Set) moreThan.get(new Integer(x))).iterator();
	}
	private void markSupers(long k, int act) {
		short spo[] = expand(k);
		boolean ok = false;
		Iterator s = supers(spo[0]);
		while (s.hasNext()) {
			Integer ss = (Integer) s.next();
			Iterator p = supers(spo[1]);
			while (p.hasNext()) {
				Integer pp = (Integer) p.next();
				Iterator o = supers(spo[2]);
				while (o.hasNext()) {
					Integer oo = (Integer) o.next();
					Long ll = toLong(ss, pp, oo);
					if (allActions(((Long) huge.get(ll)).longValue()) == act)
						plusPlus(ll);
					ok = ok || (ll.longValue() == k);
				}
			}
		}
		if (!ok)
			throw new BrokenException("impossible");
	}
	Map count = new HashMap();
	/**
	 * @param long1
	 */
	private void plusPlus(Long k) {
		int c[] = (int[]) count.get(k);
		if (c == null) {
			c = new int[] { 0 };
			count.put(k, c);
		}
		c[0]++;
	}

	/**
	 * @param ss
	 * @param pp
	 * @param oo
	 */
	private Long toLong(Integer ss, Integer pp, Integer oo) {
		return toLong(ss.intValue(), pp.intValue(), oo.intValue());
	}

	private void findUseless() {
		int cnt = 0;
		int bad = 0;
		Iterator it = huge.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry ent = (Map.Entry) it.next();
			markSupers(
				((Long) ent.getKey()).longValue(),
				allActions(((Long) ent.getValue()).longValue()));
		}
		it = huge.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry ent = (Map.Entry) it.next();
			if (isUseless((Long) ent.getKey())) {
				cnt++;
				long old = ((Long) ent.getValue()).longValue();
				long newv = (long) RemoveTriple << (long) (3L * W) | old;
				if (subject(old) == subject(newv)
					&& prop(old) == prop(newv)
					&& object(old) == prop(newv)
					&& (allActions(old) | RemoveTriple) == allActions(newv)) {
					ent.setValue(new Long(newv));
				} else {
					if (
					foo(subject(old), subject(newv))
					&&
					foo(prop(old), prop(newv))
					&&
					foo(object(old), object(newv))
					&&
					foo((allActions(old) | RemoveTriple), allActions(newv)) ) {
						//System.err.println("Hmm not so bad.");
						ent.setValue(new Long(newv));
					} else {
						bad++;
						System.err.println(
							bad
								+ " Error: "
								+ Long.toHexString(old)
								+ " != "
								+ Long.toHexString(newv));
					}
					

				}
			}

		}
		System.err.println(cnt + " marked as remove");
	}
	private boolean foo(int a, int b) {
		boolean rslt = a==b;
		if (!rslt)
		System.err.println(rslt + " : " + a + " == " + b);
		return rslt;
	}
	/**
	 * @param l
	 * @return
	 */
	static private int cycles[] =
		{ Grammar.cyclic, Grammar.cyclicRest, Grammar.cyclicFirst };
	private boolean isUseless(Long l) {
		short spo[] = expand(l.longValue());
		int cnt = ((int[]) count.get(l))[0];
		if (cnt != nLessThan(spo[0]) * nLessThan(spo[1]) * nLessThan(spo[2]))
			return false;
		spo = expand(((Long) huge.get(l)).longValue());
		if (Q.intersect(CategorySet.getSet(spo[0]), cycles))
			return false;
		if (Q.intersect(CategorySet.getSet(spo[2]), cycles))
			return false;
		return true;
	}

	/**
	 * @param s
	 */
	private int nLessThan(int s) {
		return ((Set) lessThan.get(new Integer(s))).size();

	}

	private short[] expand(long k) {
		return new short[] {
			(short) subject(k),
			(short) prop(k),
			(short) object(k),
			(short) allActions(k)};
	}
	public static void main(String[] args) {
		Compiler c = new Compiler();
		c.go();
	}

	private static short allActions(long k) {
		return (short) (k >> (long) (3 * W));
	} /**
					* 
					* @param refinement The result of {@link #refineTriple(int,int,int)}
					* @param subj The old subcategory for the subject.
					* @return The new subcategory for the subject.
					*/
	static private int subject(long refinement) {
		return (int) (refinement >> (2 * W)) & M;
	}
	/**
		* 
		* @param refinement The result of {@link #refineTriple(int,int,int)}
		* @param prop The old subcategory for the property.
		* @return The new subcategory for the property.
		*/
	static private int prop(long refinement) {
		return (int) (refinement >> (1 * W)) & M;
	}
	/**
		* 
		* @param refinement The result of {@link #refineTriple(int,int,int)}
		* @param obj The old subcategory for the object.
		* @return The new subcategory for the object.
		*/
	static private int object(long refinement) {
		return (int) (refinement >> (0 * W)) & M;
	}
	/**
		* @param r0
		* @return
		*/
	private static String toString(long r0) {
		if (r0 == -1)
			return "F";
		return "S"
			+ CategorySet.catString(subject(r0))
			+ " P"
			+ CategorySet.catString(prop(r0))
			+ " O"
			+ CategorySet.catString(object(r0));
	}
	/**
		* @param long1
		* @return
		*/
	private static String toString(Long long1) {
		return toString(long1.longValue());
	}

	static private class Pair implements Comparable {
		final int a, b;
		Pair(int A, int B) {
			if (A < B) {
				a = A;
				b = B;
			} else {
				a = B;
				b = A;
			}
		} /*
																													 * (non-Javadoc)
																													 * 
																													 * @see java.lang.Comparable#compareTo(java.lang.Object)
																													 */
		public int compareTo(Object o) {
			Pair p = (Pair) o;
			int rslt = a - p.a;
			if (rslt == 0)
				rslt = b - p.b;
			return rslt;
		}
		public boolean equals(Object o) {
			return o instanceof Pair && compareTo(o) == 0;
		}
	}
}

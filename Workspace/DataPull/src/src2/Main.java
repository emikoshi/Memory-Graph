package src2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.swing.JFrame;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.Field;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.LongValue;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.VoidValue;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.ReferenceType;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

public class Main {
	public static HashMap<Long, ContentStructure> idhash = new HashMap<Long, ContentStructure>();
	
	// Make sure to compile before, running the JVM
	// java -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n <Classname>
	
	// http://www.jgraph.org/forum/viewtopic.php?f=13&t=5344
	
	
	public static void main (String args[]) {
		//get the virtual machine from create_vm with the specified parameters
		VirtualMachine target_vm = create_vm("com.sun.jdi.SocketAttach", 8000);
	
		//Printing out the contents of the VM, however; is not needed this far into the project.
		//print_vm_contents(target_vm);
		// //System.out.println("\nStarting to store contents...");
		// //System.out.println("\nPrinting the ContentStructure we gathered from the VM...");
		
		ContentStructure cs = store_vm_contents(target_vm);
		print_content_structure(cs,"");

		// //System.out.println("Graphing now...");
		Grapher frame = new Grapher(cs);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1000, 900);
		frame.setVisible(true);
		// //System.out.println("WEGSBFDSBDHDHDSFHDSFHD");
		//System.out.println(idhash.toString());
	}

	private static VirtualMachine create_vm(String connectorName, int port) {
		//Grab the virtual machine and create the Bootstrap virtual machine manager.
		//It will allow us to connect to the virtual machine running the target program
		//and the debugger associated with the target program.
		VirtualMachineManager CreateBoot = Bootstrap.virtualMachineManager();

		//Create a list of AttachingConnectors to choose which one to connect to 
		List<AttachingConnector> attaching_connectors_list = CreateBoot.attachingConnectors();
		
		
		//This is used in order to attach to the SocketAttach Connector
		for (AttachingConnector i:attaching_connectors_list) {
			//If the current AttachingConnector i has the name "com.sun.jdi.SocketAttach"
			if (i.name().equals(connectorName)) {
				//Output to confirm, used for debugging
				//// //System.out.println("Attaching to... " + i.name());
			
				//Set a map default_connector_map equal to the default Connector map, output the values.
				//Again used for debugging.
				java.util.Map<String, Connector.Argument> default_connector_map = i.defaultArguments();
				
				//Printed out the connector map's default values, used for debugging.
				//// //System.out.println(default_connector_map.values());

				//Setting the port to 8000, the same as is used in terminal to start up the JVM
				Connector.IntegerArgument arg = (Connector.IntegerArgument)default_connector_map.get("port");
				arg.setValue(port);
				
				try {
					//Used to print the new connector map of SocketAttach, purely debugging.
					//// //System.out.println(default_connector_map);
					// when we've finished this part, a virtual machine is returned 
					return i.attach(default_connector_map);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalConnectorArgumentsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	//This method returns a ContentStructure containing all of the threads, stacks, and variables within this program
	//Possible problems: infinite loop if there is a cycle in the graph (a sequence of nodes that point to each other).
	// virtualmachine target_vm is being passed in 
	
	private static ContentStructure store_vm_contents(VirtualMachine target_vm) {
		ContentStructure head = new ContentStructure("Base", null, "head", 0l, 0l, new ArrayList<ContentStructure>(), target_vm);
		
		
		for (ThreadReference i:target_vm.allThreads()) {
			i.suspend();
			if (!(i.name().equals("Reference Handler") || i.name().equals("Finalizer") || i.name().equals("Signal Dispatcher"))) {
				ContentStructure new_thread_cs = new ContentStructure(i.name(), "", "thread", i.uniqueID(), 0l, new ArrayList<ContentStructure>(), i);				
				head.addLink(new_thread_cs, "thread");
				//Here we might want to say that new_thread_cs is pointed to BY head, like another array to hold that data
				
				int stack_number = 0;
				try {
					//Adding the stack frames to the content structure and displaying them
					for (StackFrame j:i.frames()) {						
						ContentStructure new_stack_cs = new ContentStructure(j.toString(), "", "stack_frame", (long)stack_number, 0l, new ArrayList<ContentStructure>(), j);
						stack_number++;
						new_thread_cs.addLink(new_stack_cs, "");
						try {
							//for each local variable in the stack frame,we are retrieving
							//the visible variables
							for (LocalVariable k:j.visibleVariables()) {
								Value v = j.getValue(k);
								HashSet<Value> seen = new HashSet<Value>();
								//if the value is primitive value,goes through each type
								//of primitive values and prints those types to the content structure
								// //System.out.println("The current LocalVariable is " + k.name());
								if (v instanceof PrimitiveValue){	
									PrimitiveValue pv = null;
									pv = ((PrimitiveValue) v);
															
									// check instanceof primitive
									// add to the cs
									ContentStructure new_variable_cs = check_isntanceof_primitives(pv, k);
									new_stack_cs.addLink(new_variable_cs, k.name());									
								}
								//It checks for an instance of Object reference
								//prints the type as well as the unique ID for each variable 
								if (v instanceof ObjectReference){
									// //System.out.println(k.name() + " is an object. The object type is " + v.type().toString());
									ObjectReference vRef = (ObjectReference)v;
									if (v instanceof ArrayReference){
										List<Value> arrval = ((ArrayReference) v).getValues();
										Object[] arr = arrval.toArray();										
										// //System.out.println("We are in the ArrayReference code. The type of this object is " + v.type().toString());
										if (arr.length < 1){											
											ContentStructure new_variable_cs = new ContentStructure(k.name(), "[]", v.type().toString(), (long)k.hashCode(), vRef.uniqueID(), new ArrayList<ContentStructure>(), k);
											// if our hash does not already contain this key, add it to the hash and create a new contentstrcuture
											if (!idhash.containsKey(vRef.uniqueID())) {
												idhash.put(vRef.uniqueID(), new_variable_cs);
												new_stack_cs.addLink(new_variable_cs, k.name());
											}
											// otherwise, we can just add the existing contentstructure to the contents of the stack content structure
											else {												
												new_stack_cs.addLink(idhash.get(vRef.uniqueID()), k.name());
											}
											//// //System.out.println(((ArrayReference) ob).getValues());
										}
										else{
											//for (int u=0; u<arr.length; u++){												
												ContentStructure new_variable_cs = new ContentStructure(k.name()+"[]", "1", v.type().toString(), (long)k.hashCode(), vRef.uniqueID(), new ArrayList<ContentStructure>(), k);
												if (!idhash.containsKey(vRef.uniqueID())) {
													try {
														//System.out.println("we are going to do a DFS on " + k.name());
														object_dfs(new_variable_cs,v,seen, "  ");
														new_stack_cs.addLink(new_variable_cs, new_variable_cs.name);
														idhash.put(vRef.uniqueID(), new_variable_cs);
														
													} catch (ClassNotLoadedException e) {
														// TODO Auto-generated catch block
														//e.printStackTrace();
													}
													//new_stack_cs.contents.add(new_variable_cs);
												}
												// otherwise, we can just add the existing contentstructure to the contents of the stack content structure
												else {
													new_stack_cs.addLink(idhash.get(vRef.uniqueID()),new_variable_cs.name);
												}
												
												//// //System.out.println("Element " + u + " of "+k.name()+ " = "+ listarr[u]);
											//}
										}
									} else {
										ContentStructure new_variable_cs = new ContentStructure(k.name(), v.toString(), v.type().toString(), (long)k.hashCode(), vRef.uniqueID(), new ArrayList<ContentStructure>(), k);
										if (!idhash.containsKey(vRef.uniqueID())) {
											try {
												//System.out.println("we are going to do a DFS on " + k.name());
												object_dfs(new_variable_cs,v,seen, "  ");
												new_stack_cs.addLink(new_variable_cs,new_variable_cs.name);
												idhash.put(vRef.uniqueID(), new_variable_cs);
											} catch (ClassNotLoadedException e) {
												//e.printStackTrace();
											}
										}
										else {
											new_stack_cs.addLink(idhash.get(vRef.uniqueID()),new_variable_cs.name);
										}
	//									// //System.out.println("This objects unique ID is " + ((ObjectReference) v).uniqueID());
	
	//									ReferenceType reft = ob.referenceType();
	//									List<Field> flist = reft.fields();
	//									// //System.out.println(k.name() + " is a reference of " + reft.name());
	//									// //System.out.println(k.name() + " = "+ob.getValues(flist));
									}
								}
//								ObjectReference ob = ((ObjectReference) v);
						
//								if (ob instanceof StringReference){
//									//// //System.out.println(((StringReference) ob).value());
//								}
//								if (ob instanceof ClassObjectReference){
//									//// //System.out.println("This is a ClassObjectReference:");
//									
//								}
//								if (ob instanceof ClassLoaderReference){
//									//// //System.out.println("This is a ClassLoaderReference:");
//								}
								
								//// //System.out.println();
//								ContentStructure new_variable_cs = new ContentStructure(k.name(), j.getValue(k).toString(), k.typeName(), (long)k.hashCode(), (long)k.hashCode(), new ArrayList<ContentStructure>(), k);
//								new_stack_cs.contents.add(new_variable_cs);
								
								//// //System.out.println();
							}
						} catch (AbsentInformationException e) {
							
						}
					}
				} catch (IncompatibleThreadStateException e) {
					// TODO Auto-generated catch blocks
				}
			}
		}		
		return head;
		
	}
	//function used to print the content structure
	public static void print_content_structure(ContentStructure cs, String spaces) {
		HashSet<ContentStructure> seen = new HashSet<ContentStructure>();
		if (cs.names.size() > 0) System.out.println(spaces+cs.names);
		//if (cs.name.contains("ArrayList"))
			System.out.println(spaces + cs.type + " " + cs.name + " uid=" + cs.uid);	
		for (ContentStructure i:cs.contents) {
			if (!seen.contains(i)) {
				print_content_structure(i, spaces+"   ");		
				seen.add(i);
			} 
		}
	}	
	private static void object_dfs(ContentStructure cs, Value v, HashSet<Value> seen, String spaces) throws ClassNotLoadedException {
		//make DFS
		//ContentStructure new_stack_cs = new ContentStructure(j.toString(), null, "stack_frame", (long)stack_number, 0l, new ArrayList<ContentStructure>(), j);
		ObjectReference ob = null;
		ob = ((ObjectReference) v);
		
		// //System.out.println(spaces+ ob+" "+v);
	
		ReferenceType reft = ob.referenceType();		
		List<Field> flist = reft.fields();
		
		//// //System.out.println("hi");
		//// //System.out.println("I added: " + v.type().name() + ob.getValues(flist).toString());
		if (v instanceof ArrayReference){
			ArrRefCheck (cs,seen,v,spaces);
		}
		else if (v instanceof PrimitiveValue){
			//System.out.println("!!!!!!!!!!");
		}
		else if (!v.type().name().contains("java.lang")){			
			//System.out.println(spaces+"It was not an arrayReference");
			for (Field fld: flist){
				// //System.out.println(spaces+"This is the list of fields: "+flist);
				 //System.out.println(spaces+"We're on field: " + fld + " with type" + fld.typeName());
				 
				if (ob.getValue(fld) != null){
					//// //System.out.println("Field=" + ob.getValue(fld).type() + " is not null");
					if (ob.getValue(fld) instanceof ObjectReference){
						ObjectReference obRef = (ObjectReference)ob.getValue(fld);
						//System.out.println(spaces+"We have found an object reference within the field list. Its value is ");
						//// //System.out.println("Seen is: "+seen);
						if (seen.contains(ob.getValue(fld))) {
							//System.out.println(spaces+"We've found a seen value. It's exiting");
							ContentStructure new_variable_cs = new ContentStructure(flist.get(flist.indexOf(fld)).name(), ob.getValue(fld).toString(), fld.type().toString(), (long)fld.hashCode(), obRef.uniqueID(), new ArrayList<ContentStructure>(), v);
							if (!idhash.containsKey(obRef.uniqueID())) {
								idhash.put(obRef.uniqueID(), new_variable_cs);
								cs.addLink(new_variable_cs,new_variable_cs.name);
							}
							// otherwise, we can just add the existing contentstructure to the contents of the stack content structure
							else {
								cs.addLink(idhash.get(obRef.uniqueID()),new_variable_cs.name);
							}
							
							
						} else {
							//System.out.println(spaces+"the type of this field is "+fld.typeName().toString());
							////System.out.println(ob.type().name());
							// i took out && !ob.type().name().startsWith("java.lang.Object[]") from the if statement below
							if (ob.type().name().startsWith("java.lang") ){
								// //System.out.println(spaces+"We've reached a java.lang, adding to seen + content structure, then ignoring it");
								ContentStructure new_variable_cs = new ContentStructure(flist.get(flist.indexOf(fld)).name(), ob.getValue(fld).toString(), fld.type().toString(), (long)fld.hashCode(), obRef.uniqueID(), new ArrayList<ContentStructure>(), v);
								if (!idhash.containsKey(obRef.uniqueID())) {
									idhash.put(obRef.uniqueID(), new_variable_cs);
									cs.addLink(new_variable_cs,new_variable_cs.name);
								}
								// otherwise, we can just add the existing contentstructure to the contents of the stack content structure
								else {
									cs.addLink(idhash.get(obRef.uniqueID()),new_variable_cs.name);
								}
								seen.add(ob.getValue(fld));
								//break;
							}
							else {
								//System.out.println(spaces+"we're going to DFS through the current field, it is not a java.lang ...");
								seen.add(ob.getValue(fld));
								ContentStructure new_variable_cs = new ContentStructure(flist.get(flist.indexOf(fld)).name(), ob.getValue(fld).toString(), fld.type().toString(), (long)fld.hashCode(), obRef.uniqueID(), new ArrayList<ContentStructure>(), v);
								if (!idhash.containsKey(ob.uniqueID())) {									
									//System.out.println(spaces+"THE VALUE IS BEING ADDED: "+ reft.allFields());
									object_dfs(new_variable_cs,ob.getValue(fld),seen, spaces + "  ");
									idhash.put(obRef.uniqueID(), new_variable_cs);
									cs.addLink(new_variable_cs,new_variable_cs.name);
								}
								// otherwise, we can just add the existing contentstructure to the contents of the stack content structure
								else {
									cs.addLink(idhash.get(obRef.uniqueID()),new_variable_cs.name);
								}
							}
						}
					} else if(ob.getValue(fld) instanceof PrimitiveValue){
						//System.out.println(spaces+"PRIMVAL inside field");
						PrimitiveValue pv = (PrimitiveValue)ob.getValue(fld);
						if (ob.getValue(fld) instanceof BooleanValue){
							ContentStructure new_variable_cs = new ContentStructure(flist.get(flist.indexOf(fld)).name(), ""+pv.booleanValue(), pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
							cs.addLink(new_variable_cs, new_variable_cs.name);
							// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is BooleanValue, with a value of: "+pv.booleanValue());
						}
						if (ob.getValue(fld) instanceof ByteValue){
							ContentStructure new_variable_cs = new ContentStructure(flist.get(flist.indexOf(fld)).name(), ""+pv.byteValue(), pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
							cs.addLink(new_variable_cs,new_variable_cs.name);
							// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is ByteValue, with a value of: "+pv.byteValue());
						}
						if (ob.getValue(fld) instanceof CharValue){
							ContentStructure new_variable_cs = new ContentStructure(flist.get(flist.indexOf(fld)).name(), ""+pv.charValue(), pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
							cs.addLink(new_variable_cs,new_variable_cs.name);
							// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is CharValue, with a value of: "+pv.charValue());
						}
						if (ob.getValue(fld) instanceof DoubleValue){
							ContentStructure new_variable_cs = new ContentStructure(flist.get(flist.indexOf(fld)).name(), ""+pv.doubleValue(), pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
							cs.addLink(new_variable_cs,new_variable_cs.name);
							// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is DoubleValue, with a value of: "+pv.doubleValue());
						}
						if (ob.getValue(fld) instanceof FloatValue){
							ContentStructure new_variable_cs = new ContentStructure(flist.get(flist.indexOf(fld)).name(), ""+pv.floatValue(), pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
							cs.addLink(new_variable_cs,new_variable_cs.name);
							// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is FloatValue, with a value of: "+pv.floatValue());
						}
						if (ob.getValue(fld) instanceof IntegerValue){
							ContentStructure new_variable_cs = new ContentStructure(flist.get(flist.indexOf(fld)).name(), ""+pv.intValue(), pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
							cs.addLink(new_variable_cs,new_variable_cs.name);
							// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is IntegerValue, with a value of: "+pv.intValue());
						}
						if (ob.getValue(fld) instanceof LongValue){
							ContentStructure new_variable_cs = new ContentStructure(flist.get(flist.indexOf(fld)).name(), ""+pv.longValue(), pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
							cs.addLink(new_variable_cs,new_variable_cs.name);
							// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is LongValue, with a value of: "+pv.longValue());
						}
						if (ob.getValue(fld) instanceof ShortValue){
							ContentStructure new_variable_cs = new ContentStructure(flist.get(flist.indexOf(fld)).name(), ""+pv.shortValue(), pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
							cs.addLink(new_variable_cs,new_variable_cs.name);
							// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is ShortValue, with a value of: "+pv.shortValue());
						}
						if (ob.getValue(fld) instanceof VoidValue){
							ContentStructure new_variable_cs = new ContentStructure(flist.get(flist.indexOf(fld)).name(), "VOID", pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
							cs.addLink(new_variable_cs,new_variable_cs.name);
							// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is VoidValue, and is...VOID");
						}			
					
					}
					else if (v instanceof ArrayReference){
						//System.out.println(spaces +"ARRREF inside field");
						ArrRefCheck (cs,seen,v,spaces);
					}
				} 
					else if (ob.getValue(fld) == null) {
					//System.out.println(spaces+ ob.getValue(fld)+"This data is null. Moving on to the next field.");
				} 
				else {
					//System.out.println(spaces+"This data is a primitive value");			
					//System.out.println(spaces + "We have detected a PrimitiveValue. Adding it to the contentStructure.");
					PrimitiveValue pv = (PrimitiveValue)ob.getValue(fld);
					if (ob.getValue(fld) instanceof BooleanValue){
						ContentStructure new_variable_cs = new ContentStructure(fld.type().name(), ""+pv.booleanValue(), pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
						cs.addLink(new_variable_cs,new_variable_cs.name);
						// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is BooleanValue, with a value of: "+pv.booleanValue());
					}
					if (ob.getValue(fld) instanceof ByteValue){
						ContentStructure new_variable_cs = new ContentStructure(fld.type().name(), ""+pv.byteValue(), pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
						cs.addLink(new_variable_cs,new_variable_cs.name);
						// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is ByteValue, with a value of: "+pv.byteValue());
					}
					if (ob.getValue(fld) instanceof CharValue){
						ContentStructure new_variable_cs = new ContentStructure(fld.type().name(), ""+pv.charValue(), pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
						cs.addLink(new_variable_cs,new_variable_cs.name);
						// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is CharValue, with a value of: "+pv.charValue());
					}
					if (ob.getValue(fld) instanceof DoubleValue){
						ContentStructure new_variable_cs = new ContentStructure(fld.type().name(), ""+pv.doubleValue(), pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
						cs.addLink(new_variable_cs,new_variable_cs.name);
						// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is DoubleValue, with a value of: "+pv.doubleValue());
					}
					if (ob.getValue(fld) instanceof FloatValue){
						ContentStructure new_variable_cs = new ContentStructure(fld.type().name(), ""+pv.floatValue(), pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
						cs.addLink(new_variable_cs,new_variable_cs.name);
						// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is FloatValue, with a value of: "+pv.floatValue());
					}
					if (ob.getValue(fld) instanceof IntegerValue){
						ContentStructure new_variable_cs = new ContentStructure(fld.type().name(), ""+pv.intValue(), pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
						cs.addLink(new_variable_cs,new_variable_cs.name);
						// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is IntegerValue, with a value of: "+pv.intValue());
					}
					if (ob.getValue(fld) instanceof LongValue){
						ContentStructure new_variable_cs = new ContentStructure(fld.type().name(), ""+pv.longValue(), pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
						cs.addLink(new_variable_cs,new_variable_cs.name);
						// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is LongValue, with a value of: "+pv.longValue());
					}
					if (ob.getValue(fld) instanceof ShortValue){
						ContentStructure new_variable_cs = new ContentStructure(fld.type().name(), ""+pv.shortValue(), pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
						cs.addLink(new_variable_cs,new_variable_cs.name);
						// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is ShortValue, with a value of: "+pv.shortValue());
					}
					if (ob.getValue(fld) instanceof VoidValue){
						ContentStructure new_variable_cs = new ContentStructure(fld.type().name(), "VOID", pv.type().toString(), (long)fld.hashCode(), 0l, new ArrayList<ContentStructure>(), fld);
						cs.addLink(new_variable_cs,new_variable_cs.name);
						// //System.out.println(spaces+fld.name()+" is a PrimitiveValue. Its type is VoidValue, and is...VOID");
					}						
				}
			}
		}
	}
	public static ContentStructure check_isntanceof_primitives(PrimitiveValue pv, LocalVariable k) {
		if (pv instanceof BooleanValue){
			// //System.out.println(k.name()+" is a PrimitiveValue. Its type is BooleanValue, with a value of: "+pv.booleanValue());		
			return new ContentStructure(k.name(), ""+pv.booleanValue(), pv.type().toString(), (long)k.hashCode(), 0l, new ArrayList<ContentStructure>(), k);			
		}
		if (pv instanceof ByteValue){
			// //System.out.println(k.name()+" is a PrimitiveValue. Its type is ByteValue, with a value of: "+pv.byteValue());
			return new ContentStructure(k.name(), ""+pv.byteValue(), pv.type().toString(), (long)k.hashCode(), 0l, new ArrayList<ContentStructure>(), k);
		}
		if (pv instanceof CharValue){
			// //System.out.println(k.name()+" is a PrimitiveValue. Its type is CharValue, with a value of: "+pv.charValue());
			return new ContentStructure(k.name(), ""+pv.charValue(), pv.type().toString(), (long)k.hashCode(), 0l, new ArrayList<ContentStructure>(), k);		
		}
		if (pv instanceof DoubleValue){
			// //System.out.println(k.name()+" is a PrimitiveValue. Its type is DoubleValue, with a value of: "+pv.doubleValue());
			return new ContentStructure(k.name(), ""+pv.doubleValue(), pv.type().toString(), (long)k.hashCode(), 0l, new ArrayList<ContentStructure>(), k);		
		}
		if (pv instanceof FloatValue){
			// //System.out.println(k.name()+" is a PrimitiveValue. Its type is FloatValue, with a value of: "+pv.floatValue());
			return new ContentStructure(k.name(), ""+pv.floatValue(), pv.type().toString(), (long)k.hashCode(), 0l, new ArrayList<ContentStructure>(), k);		
		}
		if (pv instanceof IntegerValue){
			// //System.out.println(k.name()+" is a PrimitiveValue. Its type is IntegerValue, with a value of: "+pv.intValue());
			return new ContentStructure(k.name(), ""+pv.intValue(), pv.type().toString(), (long)k.hashCode(), 0l, new ArrayList<ContentStructure>(), k);		
		}
		if (pv instanceof LongValue){
			// //System.out.println(k.name()+" is a PrimitiveValue. Its type is LongValue, with a value of: "+pv.longValue());
			return new ContentStructure(k.name(), ""+pv.longValue(), pv.type().toString(), (long)k.hashCode(), 0l, new ArrayList<ContentStructure>(), k);		
		}
		if (pv instanceof ShortValue){		
			// //System.out.println(k.name()+" is a PrimitiveValue. Its type is ShortValue, with a value of: "+pv.shortValue());
			return new ContentStructure(k.name(), ""+pv.shortValue(), pv.type().toString(), (long)k.hashCode(), 0l, new ArrayList<ContentStructure>(), k);
		}
		if (pv instanceof VoidValue){		
			// //System.out.println(k.name()+" is a PrimitiveValue. Its type is VoidValue, and is...VOID");
			return new ContentStructure(k.name(), "VOID", pv.type().toString(), (long)k.hashCode(), 0l, new ArrayList<ContentStructure>(), k);
		}
		return null;
	}	
	
	public static void ArrRefCheck (ContentStructure cs,HashSet<Value> seen,Value v, String spaces) throws ClassNotLoadedException{
		//System.out.println(spaces+"We are handling an ArrayReference.");
		List<Value> arrval = ((ArrayReference) v).getValues();
		Object[] arr = arrval.toArray();
		for (int u=0; u<arr.length; u++){
			if (u >= 5 && u <= arr.length - 5) continue;
			
			//ContentStructure new_variable_cs = new ContentStructure(cs.name+"["+u+"]", arr[u].toString(), v.type().toString(), (long)arr.hashCode(), 0l, new ArrayList<ContentStructure>(),v);
			// //System.out.println(spaces+"arr[u] = " + arr[u]);
			if (arrval.get(u) instanceof PrimitiveValue){
				// //System.out.println(spaces + "We have detected a PrimitiveValue. Adding it to the contentStructure.");
				ContentStructure tempCS = new ContentStructure(cs.name+"["+u+"]", arr[u].toString(), arrval.get(u).type().toString(), (long)arr.hashCode(), 0l, new ArrayList<ContentStructure>(),v);
				cs.addLink(tempCS, cs.name+"["+Integer.toString(u)+"]");
			}
			else if (arr[u] != null){
				// Look for stuff
				ObjectReference ob = (ObjectReference)arr[u];
				ContentStructure new_variable_cs = new ContentStructure(cs.name+"["+u+"]", arr[u] == null ? "" : arr[u].toString(), arr[u].toString(), 
						(long)arr.hashCode(), ob.uniqueID(), new ArrayList<ContentStructure>(),v);
				if (!idhash.containsKey(ob.uniqueID())) {									
					//System.out.println(spaces+"THE VALUE IS BEING ADDED: "+ reft.allFields());
					object_dfs(new_variable_cs,arrval.get(u),seen, spaces + "  ");
					idhash.put(ob.uniqueID(), new_variable_cs);
					cs.addLink(new_variable_cs, new_variable_cs.name);
				}
				// otherwise, we can just add the existing contentstructure to the contents of the stack content structure
				else {
					cs.addLink(idhash.get(ob.uniqueID()),new_variable_cs.name);
				}
				//System.out.println(spaces+"There is an object within this ArrayReference " + cs.name +"We must DFS again. " + arrval);

						
				//System.out.println(spaces+"THE VALUE: "+ arrval.get(u));

				//cs.contents.add(new ContentStructure(cs.name+"["+u+"]", arr[u].toString(), arrval.get(u).type().toString(), (long)arr.hashCode(), 0l, new ArrayList<ContentStructure>(),v));

			}
		}
	}
}
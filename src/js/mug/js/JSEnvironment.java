package mug.js;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSEnvironment {
	/*
	 * prototypes
	 */
	
	final JSObject objectPrototype = new JSObject(this);
	
	final JSNull nullObject = new JSNull(this);
	
	final JSObject functionPrototype = new JSObject(this) { {
		// needed to reference self
		JSObject functionPrototype = this;

		set("apply", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				JSObject thsObj = JSUtils.asJSObject(JSEnvironment.this, ths);
				return thsObj.invoke(l0, JSUtils.coerceJavaArray(JSUtils.asJSObject(JSEnvironment.this, l1)));
			}
		});
		
		set("call", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				JSObject thsObj = JSUtils.asJSObject(JSEnvironment.this, ths);
				Object[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
				Object[] passedArgs = new Object[Math.max(arguments.length - 1, 0)];
				if (arguments.length > 1)
					System.arraycopy(arguments, 1, passedArgs, 0, arguments.length - 1);
				return thsObj.invoke(l0, passedArgs);
			}
		});
	} };
	
	{
		objectPrototype.set("toString", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				//[TODO] object classes etc.
				return "[object Object]";
			}
		});

		objectPrototype.set("hasOwnProperty", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				JSObject obj = JSUtils.asJSObject(JSEnvironment.this, ths);
				return obj.hasOwnProperty(JSUtils.asString(l0));
			}
		});
	}
	
	final JSObject arrayPrototype = new JSObject(this) { {
		set("concat", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				// concatenate arrays to new array
				JSObject thsObj = JSUtils.asJSObject(JSEnvironment.this, ths);
				JSArray out = new JSArray(JSEnvironment.this, 0);
				Object[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
				for (int j = 0, max = (int) JSUtils.asNumber(thsObj.get("length")); j < max; j++)
					out.push(thsObj.get(String.valueOf(j)));
				for (Object arg : arguments) {
					JSObject arr = JSUtils.asJSObject(JSEnvironment.this, arg);
					for (int j = 0, max = (int) JSUtils.asNumber(arr.get("length")); j < max; j++)
						out.push(arr.get(String.valueOf(j)));
				}
				return out;
			}
		});
		
		set("push", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				JSObject thsObj = (JSObject) ths;
				thsObj.set(String.valueOf((int) JSUtils.asNumber(thsObj.get("length"))), l0);
				return thsObj.get("length");
			}
		});
		
		set("pop", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				JSObject thsObj = (JSObject) ths;
				int len = (int) JSUtils.asNumber(thsObj.get("length"));
				Object out = thsObj.get(String.valueOf(len-1));
				thsObj.set("length", len-1);
				return out;
			}
		});
		
		set("map", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				JSObject thsObj = JSUtils.asJSObject(JSEnvironment.this, ths);
				JSObject func = JSUtils.asJSObject(JSEnvironment.this, l0);
				JSArray out = new JSArray(JSEnvironment.this, 0);
				int len = (int) JSUtils.asNumber(thsObj.get("length"));
				for (int i = 0; i < len; i++)
					out.push(func.invoke(ths, thsObj.get(String.valueOf(i)), i));
				return out;
			}
		});
		
		final JSFunction _indexOf = new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				JSObject thsObj = JSUtils.asJSObject(JSEnvironment.this, ths);
				int len = (int) JSUtils.asNumber(thsObj.get("length"));
				for (int i = 0; i < len; i++)
					if (JSUtils.testEquality(thsObj.get(i), l0))
						return i;
				return -1;
			}
		};
		set("indexOf", _indexOf);
		
		final JSFunction _join = new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				JSObject thsObj = JSUtils.asJSObject(JSEnvironment.this, ths);
				StringBuffer sb = new StringBuffer();
				String delim = (l0 == null || l0.equals(nullObject)) ? "" : JSUtils.asString(l0); 
				int len = (int) JSUtils.asNumber(thsObj.get("length"));
				for (int i = 0; i < len; i++) {
					if (i > 0)
						sb.append(delim);
					sb.append(JSUtils.asString(thsObj.get(i)));
				}
				return sb.toString();
			}
		};
		set("join", _join);
		
		set("slice", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				JSObject thsObj = JSUtils.asJSObject(JSEnvironment.this, ths);
				int len = (int) JSUtils.asNumber(thsObj.get("length"));
				int start = (int) JSUtils.asNumber(l0);
				int end = l1 == null ? len : (int) JSUtils.asNumber(l1);
				if (end < 0)
					end += len + 1;
				JSArray out = new JSArray(JSEnvironment.this, 0);
				for (int i = start; i < end; i++)
					out.push(thsObj.get(String.valueOf(i)));
				return out;
			}
		});
		
		set("toString", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return _join.invoke(ths, ",");
			}
		});
		
		set("sort", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(final Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				// cast to strings
				Object[] arr = JSUtils.coerceJavaArray((JSObject) ths);
				for (int i = 0; i < arr.length; i++)
					arr[i] = JSUtils.asString(arr[i]);
				// sort
				final JSObject func = l0 != null ? (JSObject) l0 :
					new JSFunction(JSEnvironment.this) {
						@Override
						public Object invoke(final Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
						{
							return ((String) l0).compareTo((String) l1);
						}
					};
				Arrays.sort(arr, new Comparator<Object>() {
					@Override
					public int compare(Object a, Object b) {
						try {
							return (int) JSUtils.asNumber(func.invoke(ths, a, b));
						} catch (Exception e) {
							return 0;
						}
					}
				});
				// return new array
				JSArray out = new JSArray(JSEnvironment.this, 0);
				out.append(arr);
				return out;
			}
		});
	} };
	
	final JSObject stringPrototype = new JSObject(this) { {
		set("valueOf", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				if (ths instanceof JSString)
					return ((JSString) ths).value;
				return ths.toString();
			}
		});
		
		set("charAt", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				int idx = (int) JSUtils.asNumber(l0);
				String str = JSUtils.asString(ths);
				return idx >= 0 && idx < str.length() ? String.valueOf(str.charAt(idx)) : "";
			}
		});
		
		set("charCodeAt", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				int idx = (int) JSUtils.asNumber(l0);
				String str = JSUtils.asString(ths);
				return idx >= 0 && idx < str.length() ? (double) str.charAt(idx) : Double.NaN;
			}
		});
		
		set("replace", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				String value = JSUtils.asString(l1);
				if (l0 instanceof JSRegExp) {
					JSRegExp regexp = (JSRegExp) l0;
					Matcher matcher = regexp.getPattern().matcher(JSUtils.asString(ths));
					return regexp.isGlobal() ? matcher.replaceAll(value) : matcher.replaceFirst(value);
				} else {
					String match = JSUtils.asString(l0);
					return JSUtils.asString(ths).replaceFirst(Pattern.quote(match), value);
				}
			}
		});
		
		set("substring", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				String value = JSUtils.asString(ths);
				int start = (int) JSUtils.asNumber(l0);
				int end = l1 == null ? value.length() : (int) JSUtils.asNumber(l1);
				return value.substring(start, end);
			}
		});
		
		set("substr", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				String value = JSUtils.asString(ths);
				int start = (int) JSUtils.asNumber(l0);
				int end = l1 == null ? value.length() : ((int) JSUtils.asNumber(l1)) + start;
				return value.substring(start, end);
			}
		});
		
		set("indexOf", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				String value = JSUtils.asString(ths);
				String search = JSUtils.asString(l0);
				return value.indexOf(search);
			}
		});
		
		set("toLowerCase", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object index, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return JSUtils.asString(ths).toLowerCase();
			}
		});
		
		set("toUpperCase", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object index, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return JSUtils.asString(ths).toUpperCase();
			}
		});
		
		set("split", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Pattern pattern = (l0 instanceof JSRegExp) ? ((JSRegExp) l0).getPattern() : Pattern.compile(Pattern.quote(JSUtils.asString(l0)));
				String[] result = pattern.split(JSUtils.asString(ths), -1);
				// specific hack for empty strings...
				if (pattern.pattern().equals("\\Q\\E")) {
					String str = JSUtils.asString(ths);
					result = new String[str.length()];
					for (int i = 0; i < str.length(); i++)
						result[i] = String.valueOf(str.charAt(i));
				}
				JSArray out = new JSArray(JSEnvironment.this, 0);
				for (int i = 0; i < result.length; i++)
					out.push(result[i]);
				return out;
			}
		});
		
		set("match", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				//[TODO] if l0 is string, compile
				Pattern pattern = ((JSRegExp) l0).getPattern();
				JSArray out = new JSArray(JSEnvironment.this, 0);
				Matcher matcher = pattern.matcher(JSUtils.asString(ths));
				if (!((JSRegExp) l0).isGlobal()) {
					if (!matcher.find())
						return nullObject;
					for (int i = 0; i < matcher.groupCount() + 1; i++)
						out.push(matcher.group(i));
				} else {
					while (matcher.find())
						out.push(matcher.group(0));
					if (out.getLength() == 0)
						return nullObject;
				}
				return out;
			}
		});
	} };
	
	final JSObject numberPrototype = new JSObject(this) { {
		set("valueOf", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				if (ths instanceof JSNumber)
					return ((JSNumber) ths).value;
				return Double.NaN;
			}
		});

		set("toString", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				int base = (int) JSUtils.asNumber(l0);
				double value = JSUtils.asNumber(ths);
				switch (base) {
				case 2: return Integer.toBinaryString((int) value);
				case 8:	return Integer.toOctalString((int) value);
				case 16: return Integer.toHexString((int) value);
				}
				return Double.toString(value);
			}
		});
		
		set("toFixed", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				double value = JSUtils.asNumber(ths);
				int fixed = (int) JSUtils.asNumber(l0); 
				DecimalFormat formatter = new DecimalFormat("0" +
					(fixed > 0 ? "." + String.format(String.format("%%0%dd", fixed), 0) : ""));
				return formatter.format(value);
			}
		});
	} };
	
	final JSObject booleanPrototype = new JSObject(this) { {
		set("valueOf", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				if (ths instanceof JSBoolean)
					return ((JSBoolean) ths).value;
				return false;
			}
		});
	} };
	
	final JSObject regexpPrototype = new JSObject(this) { {
		set("test", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Pattern pattern = ((JSRegExp) ths).getPattern();
				return pattern.matcher(JSUtils.asString(l0)).find();
			}
		});
		
		set("exec", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Pattern pattern = ((JSRegExp) ths).getPattern();
				JSArray out = new JSArray(JSEnvironment.this, 0);
				Matcher matcher = pattern.matcher(JSUtils.asString(l0));
				if (!((JSRegExp) ths).isGlobal()) {
					if (!matcher.find())
						return nullObject;
					for (int i = 0; i < matcher.groupCount() + 1; i++)
						out.push(matcher.group(i));
				} else {
					while (matcher.find())
						out.push(matcher.group(0));
					if (out.getLength() == 0)
						return nullObject;
				}
				return out;
			}
		});
	} };
	
	final JSObject datePrototype = new JSObject(this) { {
		set("valueOf", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return ths instanceof JSDate ? ((JSDate) ths).value.getTimeInMillis() : 0;
			}
		});
		
		set("getDate", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
				return cal.get(Calendar.DATE);
			}
		});
		
		set("getDay", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
				return cal.get(Calendar.DAY_OF_WEEK);
			}
		});
		
		set("getFullYear", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
				return cal.get(Calendar.YEAR);
			}
		});
		
		set("getHours", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
				return cal.get(Calendar.HOUR_OF_DAY);
			}
		});
		
		set("getMilliseconds", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
				return cal.get(Calendar.MILLISECOND);
			}
		});
		
		set("getMinutes", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
				return cal.get(Calendar.MINUTE);
			}
		});
		
		set("getMonth", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
				return cal.get(Calendar.MONTH);
			}
		});
		
		set("getSeconds", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
				return cal.get(Calendar.SECOND);
			}
		});
		
		set("getTime", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
				return cal.getTimeInMillis();
			}
		});
		
		set("getTimezoneOffset", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
				return cal.get(Calendar.DST_OFFSET)/60;
			}
		});
		
		set("getDay", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
				return cal.get(Calendar.DAY_OF_WEEK);
			}
		});
	} };
	
	/*
	 * objects/constructors
	 */	
	
	static class CustomSecurityManager extends SecurityManager {
		public String getCallerClassName(int callStackDepth) {
			return getClassContext()[callStackDepth].getName();
		}
	}

	private final static CustomSecurityManager securityManager = new CustomSecurityManager();

	public String getCallerClassName(int callStackDepth) {
		return securityManager.getCallerClassName(callStackDepth);
	}
	
	final JSFunction requireFunction = new JSFunction(JSEnvironment.this) {
		String[] modulepath;
		{
			if (System.getProperty("mug.require.path") == null)
				modulepath = new String[] { ".", "mug.modules", "" };
			else
				modulepath = (System.getProperty("mug.module.path") + ":").split(":");
		}
		
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			String moduleName = JSUtils.asString(l0).replaceAll("-", "_").replaceAll("\\.", "/");
			
			// get originating module
			String callingPath = getCallerClassName(3).replaceFirst("\\.[a-zA-Z_]+$", "");
			
			// iterating through module paths
			for (String loc : modulepath) {
				try {
					String search = (loc.equals("") ? "" :
						(loc.equals(".") ? callingPath + "." : loc + ".")) + moduleName;
					// System.out.println(search);
					Class mdClass = JSModule.class.getClassLoader().loadClass(search);
					if (!JSModule.class.isAssignableFrom(mdClass))
						continue;
					JSModule module = (JSModule) mdClass.newInstance();
					return module.load();
				} catch (ClassNotFoundException e) {
				}
			}
			// exhausted all possibilities
			throw new ClassNotFoundException("Did not find a module \"" + moduleName + "\" in require path.");
		}
	};
	
	final JSObject exportsObject = new JSObject(this) { };
	
	final JSObject consoleObject = new JSObject(this) { {
		set("log", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Object[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
				for (int i = 0; i < arguments.length; i++) {
					if (i > 0)
						System.out.print(" ");
					System.out.print(JSON.stringify(arguments[i]));
				}
				System.out.println("");
				return null;
			}
		});
	} };
	
	final JSFunction printFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			Object[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
			for (int i = 0; i < arguments.length; i++) {
				if (i > 0)
					System.out.print(" ");
				System.out.print(JSUtils.asString(arguments[i]));
			}
			System.out.println("");
			return null;
		}
	};
	
	final JSFunction parseIntFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			return (double) Integer.parseInt(JSUtils.asString(l0));
		}
	};
	
	final JSFunction parseFloatFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			return Double.parseDouble(JSUtils.asString(l0));
		}
	};
	
	final JSFunction isNaNFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			return Double.isNaN(JSUtils.asNumber(l0));
		}
	};
	
	final JSFunction isFiniteFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			return !Double.isInfinite(JSUtils.asNumber(l0));
		}
	};
	
	final JSObject mathObject = new JSObject(this) { {
		set("random", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return Math.random();
			}
		});
		
		set("abs", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return Math.abs(JSUtils.asNumber(l0));
			}
		});
		
		set("pow", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return Math.pow(JSUtils.asNumber(l0), JSUtils.asNumber(l1));
			}
		});
		
		set("sqrt", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return Math.sqrt(JSUtils.asNumber(l0));
			}
		});
		
		set("max", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return Math.max(JSUtils.asNumber(l0), JSUtils.asNumber(l1));
			}
		});
		
		set("min", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return Math.min(JSUtils.asNumber(l0), JSUtils.asNumber(l1));
			}
		});
		
		set("ceil", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return Math.ceil(JSUtils.asNumber(l0));
			}
		});
		
		set("floor", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return Math.floor(JSUtils.asNumber(l0));
			}
		});
		
		set("round", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return Math.round(JSUtils.asNumber(l0));
			}
		});
	} };
	
	final JSObject jsonObject = new JSObject(this) { {
		set("stringify", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return JSON.stringify(l0);
			}
		});
		
		set("parse", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return JSON.parse(JSEnvironment.this, JSUtils.asString(l0));
			}
		});
	} };
	
	final JSFunction objectConstructor = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			return JSUtils.asJSObject(JSEnvironment.this, l0);
		}
		
		{
			set("prototype", getObjectPrototype());
		}
	};
	
	final JSFunction functionConstructor = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			throw new Exception("Function constructor not implemented.");
		}
		
		{
			set("prototype", getFunctionPrototype());
		}
	};
	
	final JSFunction numberConstructor = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			return JSUtils.asNumber(l0);
		}
		
		{
			set("prototype", getNumberPrototype());
		}
	};
	
	final JSFunction arrayConstructor = new JSFunction(JSEnvironment.this) {
		public Object instantiate(int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			return invoke(null, argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
		}
		
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest)
				throws Exception {
			
			// single-argument constructor
			if (argc == 1) {
				int length = (int) JSUtils.asNumber(l0);
				JSArray arr = new JSArray(JSEnvironment.this, length);
				for (int i = 0; i < length; i++)
					arr.push(null);
				return arr;
			}
			
			// literal declaration
			Object[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
			JSArray arr = new JSArray(JSEnvironment.this, arguments.length);
			for (int i = 0; i < arguments.length; i++)
				arr.push(arguments[i]);
			return arr;
		}
		
		{
			set("prototype", getArrayPrototype()); 
		}
	};
	
	final JSFunction dateConstructor = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			long time = 0;
			if (argc == 1) {
				//[TODO]
			}
			
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date(time));
			return new JSDate(JSEnvironment.this, cal);
		}
		
		{
			set("prototype", getDatePrototype()); 
		}
	};
	
	final JSFunction setTimeoutFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest)
				throws Exception {
			JSObject callback = JSUtils.asJSObject(JSEnvironment.this, l0);
			long milliseconds = (long) JSUtils.asNumber(l1);
			Object[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
			Object[] passedArgs = new Object[Math.max(arguments.length - 2, 0)];
			if (arguments.length > 2)
				System.arraycopy(arguments, 2, passedArgs, 0, arguments.length - 2);
			return JSTimers.setTimeout(callback, passedArgs, milliseconds);
		}
	};
	
	final JSFunction setIntervalFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest)
				throws Exception {
			JSObject callback = JSUtils.asJSObject(JSEnvironment.this, l0);
			long milliseconds = (long) JSUtils.asNumber(l1);
			Object[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
			Object[] passedArgs = new Object[Math.max(arguments.length - 2, 0)];
			if (arguments.length > 2)
				System.arraycopy(arguments, 2, passedArgs, 0, arguments.length - 2);
			return JSTimers.setInterval(callback, passedArgs, milliseconds);
		}
	};
	
	final JSFunction clearTimeoutFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest)
				throws Exception {
			JSTimers.clearTimeout((long) JSUtils.asNumber(l0));
			return null;
		}
	};
	
	final JSFunction clearIntervalFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest)
				throws Exception {
			JSTimers.clearInterval((long) JSUtils.asNumber(l0));
			return null;
		}
	};
	
	/*
	 * prototype accessors
	 */
	
	public JSObject getObjectPrototype() {
		return objectPrototype;
	}
	
	public JSObject getArrayPrototype() {
		return arrayPrototype;
	}
	
	public JSObject getStringPrototype() {
		return stringPrototype;
	}
	
	public JSObject getNumberPrototype() {
		return numberPrototype;
	}
	
	public JSObject getBooleanPrototype() {
		return booleanPrototype;
	}
	
	public JSObject getFunctionPrototype() {
		return functionPrototype;
	}
	
	public JSObject getRegExpPrototype() {
		return regexpPrototype;
	}
	
	public JSObject getDatePrototype() {
		return datePrototype;
	}
	
	public JSNull getNullObject() {
		return nullObject;
	}
	
	/*
	 * top-level scope accessors
	 */
	
	Object _require = requireFunction;
	public Object get_require() { return _require; }
	public void set_require(Object value) { _require = value; }
	
	Object _exports = exportsObject;
	public Object get_exports() { return _exports; }
	public void set_exports(Object value) { _exports = value; }
	
	Object _print = printFunction;
	public Object get_print() { return _print; }
	public void set_print(Object value) { _print = value; }
	
	Object _console = consoleObject;
	public Object get_console() { return _console; }
	public void set_console(Object value) { _console = value; }
	
	Object _parseFloat = parseFloatFunction;
	public Object get_parseFloat() { return _parseFloat; }
	public void set_parseFloat(Object value) { _parseFloat = value; }
	
	Object _parseInt = parseIntFunction;
	public Object get_parseInt() { return _parseInt; }
	public void set_parseInt(Object value) { _parseInt = value; }
	
	Object _isNaN = isNaNFunction;
	public Object get_isNaN() { return _isNaN; }
	public void set_isNaN(Object value) { _isNaN = value; }
	
	Object _isFinite = isFiniteFunction;
	public Object get_isFinite() { return _isFinite; }
	public void set_isFinite(Object value) { _isFinite = value; }
	
	Object _Math = mathObject;
	public Object get_Math() { return _Math; }
	public void set_Math(Object value) { _Math = value; }
	
	Object _JSON = jsonObject;
	public Object get_JSON() { return _JSON; }
	public void set_JSON(Object value) { _JSON = value; }

	Object _Object = objectConstructor;
	public Object get_Object() { return _Object; }
	public void set_Object(Object value) { _Object = value; }

	Object _Function = functionConstructor;
	public Object get_Function() { return _Function; }
	public void set_Function(Object value) { _Function = value; }

	Object _Array = arrayConstructor;
	public Object get_Array() { return _Array; }
	public void set_Array(Object value) { _Array = value; }
	
	Object _Number = numberConstructor;
	public Object get_Number() { return _Number; }
	public void set_Number(Object value) { _Number = value; }
	
	Object _Error = null;
	public Object get_Error() { return _Error; }
	public void set_Error(Object value) { _Error = value; }
	
	Object _setTimeout = setTimeoutFunction;
	public Object get_setTimeout() { return _setTimeout; }
	public void set_setTimeout(Object value) { _setTimeout = value; }
	
	Object _setInterval = setIntervalFunction;
	public Object get_setInterval() { return _setInterval; }
	public void set_setInterval(Object value) { _setInterval = value; }
	
	Object _clearTimeout = clearTimeoutFunction;
	public Object get_clearTimeout() { return _clearTimeout; }
	public void clear_clearTimeout(Object value) { _clearTimeout = value; }
	
	Object _clearInterval = clearIntervalFunction;
	public Object get_clearInterval() { return _clearInterval; }
	public void clear_clearInterval(Object value) { _clearInterval = value; }
}

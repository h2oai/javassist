/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 2004 Bill Burke. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.bytecode.annotation;

import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtPrimitiveType;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 1.4 $
 *
 **/
public class AnnotationInfo
{
   short type_index;
   LinkedHashMap members;
   HashMap memberName2Index;
   ConstPool cp;

   private AnnotationInfo()
   {

   }

   private MemberValue createMemberValue(ConstPool cp, CtClass returnType) throws javassist.NotFoundException
   {
      if (returnType.equals(CtPrimitiveType.booleanType))
      {
         return new BooleanMemberValue(cp);
      }
      else if (returnType.equals(CtPrimitiveType.byteType))
      {
         return new ByteMemberValue(cp);
      }
      else if (returnType.equals(CtPrimitiveType.charType))
      {
         return new CharMemberValue(cp);
      }
      else if (returnType.equals(CtPrimitiveType.doubleType))
      {
         return new DoubleMemberValue(cp);
      }
      else if (returnType.equals(CtPrimitiveType.floatType))
      {
         return new FloatMemberValue(cp);
      }
      else if (returnType.equals(CtPrimitiveType.intType))
      {
         return new IntegerMemberValue(cp);
      }
      else if (returnType.equals(CtPrimitiveType.longType))
      {
         return new LongMemberValue(cp);
      }
      else if (returnType.equals(CtPrimitiveType.shortType))
      {
         return new ShortMemberValue(cp);
      }
      else if (returnType.getName().equals("java.lang.Class"))
      {
         return new ClassMemberValue(cp);
      }
      else if (returnType.getName().equals("java.lang.String") || returnType.getName().equals("String"))
      {
         return new StringMemberValue(cp);
      }
      else if (returnType.isArray())
      {
         CtClass arrayType = returnType.getComponentType();
         MemberValue type = createMemberValue(cp, arrayType);
         return new ArrayMemberValue(type, cp);
      }
      else if (returnType.isInterface())
      {
         AnnotationInfo info = new AnnotationInfo(cp, returnType);
         return new AnnotationMemberValue(info, cp);
      }
      else
      {
         // treat as enum.  I know this is not typed, but JBoss has an Annotation Compiler for JDK 1.4
         // and I want it to work with that. - Bill Burke
         return new EnumMemberValue(returnType.getName(), cp);
      }
   }

   /**
    * todo Enums are not supported right now.
    * This is for creation at runtime
    * @param clazz
    */
   public AnnotationInfo(ConstPool cp, CtClass clazz) throws javassist.NotFoundException
   {

      if (!clazz.isInterface()) throw new RuntimeException("Only interfaces are allowed for AnnotationInfo creation.");
      this.cp = cp;
      // beta1 type_index = (short) cp.addClassInfo(clazz);
      type_index = (short)cp.addUtf8Info(Descriptor.toDescriptor(clazz.getName()));
      CtMethod methods[] = clazz.getDeclaredMethods();
      if (methods.length > 0)
      {
         members = new LinkedHashMap();
         memberName2Index = new HashMap();
      }
      for (int i = 0; i < methods.length; i++)
      {
         CtClass returnType = methods[i].getReturnType();
         addMemberValue(methods[i].getName(), createMemberValue(cp, returnType));
      }
   }

   private void addMemberValue(String name, MemberValue value)
   {
      short index = (short) cp.addUtf8Info(name);
      members.put(name, value);
      memberName2Index.put(name, new Short(index));
      value.cp = this.cp;
   }

   public String getAnnotationType()
   {
      String name = Descriptor.fromDescriptor(cp.getUtf8Info(type_index));
      return name;
   }

   public Set getMemberNames()
   {
      if (members == null) return null;
      return members.keySet();
   }

   public MemberValue getMemberValue(String member)
   {
      if (members == null) return null;
      return (MemberValue) members.get(member);
   }

   public static AnnotationInfo readAnnotationInfo(ConstPool cp, DataInput di) throws java.io.IOException
   {
      AnnotationInfo info = new AnnotationInfo();
      info.cp = cp;
      short type_index = di.readShort();
      info.type_index = type_index;
      short num_member_value_pairs = di.readShort();
      if (num_member_value_pairs > 0)
      {
         info.members = new LinkedHashMap();
         info.memberName2Index = new HashMap();
      }
      for (int j = 0; j < num_member_value_pairs; j++)
      {
         short member_name_index = di.readShort();
         String memberName = cp.getUtf8Info(member_name_index);
         MemberValue value = MemberValue.readMemberValue(cp, di);
         info.members.put(memberName, value);
         info.memberName2Index.put(memberName, new Short(member_name_index));
      }
      return info;
   }

   public void write(DataOutputStream dos) throws IOException
   {
      dos.writeShort(type_index);
      if (members == null)
      {
         dos.writeShort((short)0);
         return;
      }
      dos.writeShort(members.size());
      Iterator it = members.keySet().iterator();
      while (it.hasNext())
      {
         String name = (String) it.next();
         Short index = (Short) memberName2Index.get(name);
         dos.writeShort(index.shortValue());
         MemberValue value = (MemberValue) members.get(name);
         value.write(dos);
      }
   }

   public String toString()
   {
      StringBuffer buf = new StringBuffer("@");
      buf.append(getAnnotationType());
      if (members != null)
      {
         buf.append("(");
         Iterator mit = members.keySet().iterator();
         while (mit.hasNext())
         {
            String name = (String) mit.next();
            buf.append(name).append("=").append(getMemberValue(name));
            if (mit.hasNext()) buf.append(", ");
         }
         buf.append(")");
      }
      return buf.toString();
   }
}

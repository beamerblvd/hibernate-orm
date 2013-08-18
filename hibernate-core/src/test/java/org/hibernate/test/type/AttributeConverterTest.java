/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.type;

import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;
import java.sql.Clob;
import java.sql.Timestamp;
import java.sql.Types;

import org.hibernate.IrrelevantEntity;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.ClobTypeDescriptor;
import org.hibernate.type.descriptor.java.JdbcTimestampTypeDescriptor;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Tests the principle of adding "AttributeConverter" to the mix of {@link org.hibernate.type.Type} resolution
 *
 * @author Steve Ebersole
 */
public class AttributeConverterTest extends BaseUnitTestCase {
	@Test
	public void testBasicOperation() {
		Configuration cfg = new Configuration();
		SimpleValue simpleValue = new SimpleValue( cfg.createMappings() );
		simpleValue.setJpaAttributeConverterDefinition(
				new AttributeConverterDefinition( new StringClobConverter(), true )
		);
		simpleValue.setTypeUsingReflection( IrrelevantEntity.class.getName(), "name" );

		Type type = simpleValue.getType();
		assertNotNull( type );
		assertTyping( BasicType.class, type );
		AbstractStandardBasicType basicType = assertTyping( AbstractStandardBasicType.class, type );
		assertSame( ClobTypeDescriptor.INSTANCE, basicType.getJavaTypeDescriptor() );
		assertEquals( Types.CLOB, basicType.getSqlTypeDescriptor().getSqlType() );
	}

	@Test
	public void testBasicTimestampOperation() {
		Configuration cfg = new Configuration();
		SimpleValue simpleValue = new SimpleValue( cfg.createMappings() );
		simpleValue.setJpaAttributeConverterDefinition(
				new AttributeConverterDefinition( new InstantConverter(), true )
		);
		simpleValue.setTypeUsingReflection( IrrelevantInstantEntity.class.getName(), "dateCreated" );

		Type type = simpleValue.getType();
		assertNotNull( type );
		assertTyping( BasicType.class, type );
		AbstractStandardBasicType basicType = assertTyping( AbstractStandardBasicType.class, type );
		assertSame( JdbcTimestampTypeDescriptor.INSTANCE, basicType.getJavaTypeDescriptor() );
		assertEquals( Types.TIMESTAMP, basicType.getSqlTypeDescriptor().getSqlType() );
	}

	@Test
	public void testNormalOperation() {
		Configuration cfg = new Configuration();
		cfg.addAttributeConverter( StringClobConverter.class, true );
		cfg.addAnnotatedClass( Tester.class );
		cfg.addAnnotatedClass( Tester2.class );
		cfg.buildMappings();

		{
			PersistentClass tester = cfg.getClassMapping( Tester.class.getName() );
			Property nameProp = tester.getProperty( "name" );
			SimpleValue nameValue = (SimpleValue) nameProp.getValue();
			Type type = nameValue.getType();
			assertNotNull( type );
			assertTyping( BasicType.class, type );
			AbstractStandardBasicType basicType = assertTyping( AbstractStandardBasicType.class, type );
			assertSame( ClobTypeDescriptor.INSTANCE, basicType.getJavaTypeDescriptor() );
			assertEquals( Types.CLOB, basicType.getSqlTypeDescriptor().getSqlType() );
		}

		{
			PersistentClass tester = cfg.getClassMapping( Tester2.class.getName() );
			Property nameProp = tester.getProperty( "name" );
			SimpleValue nameValue = (SimpleValue) nameProp.getValue();
			Type type = nameValue.getType();
			assertNotNull( type );
			assertTyping( BasicType.class, type );
			AbstractStandardBasicType basicType = assertTyping( AbstractStandardBasicType.class, type );
			assertSame( StringTypeDescriptor.INSTANCE, basicType.getJavaTypeDescriptor() );
			assertEquals( Types.VARCHAR, basicType.getSqlTypeDescriptor().getSqlType() );
		}
	}


	@Entity
	public static class Tester {
		@Id
		private Long id;
		private String name;
	}

	@Entity
	public static class Tester2 {
		@Id
		private Long id;
		@Convert(disableConversion = true)
		private String name;
	}

	@Entity
	public static class Tester3 {
		@Id
		private Long id;
		@org.hibernate.annotations.Type( type = "string" )
		private String name;
	}

	@Converter( autoApply = true )
	public static class StringClobConverter implements AttributeConverter<String,Clob> {
		@Override
		public Clob convertToDatabaseColumn(String attribute) {
			return null;
		}

		@Override
		public String convertToEntityAttribute(Clob dbData) {
			return null;
		}
	}

	// This class is for mimicking an Instant from Java 8, which a converter might convert to a java.sql.Timestamp
	public static class Instant implements Serializable {
		private static final long serialVersionUID = 1L;
	}

	@Converter( autoApply = true )
	public static class InstantConverter implements AttributeConverter<Instant, Timestamp> {

		@Override
		public Timestamp convertToDatabaseColumn(Instant attribute) {
			return null;
		}

		@Override
		public Instant convertToEntityAttribute(Timestamp dbData) {
			return null;
		}
	}

	@Entity
	public class IrrelevantInstantEntity {
		private Integer id;
		private Instant dateCreated;

		@Id
		@GeneratedValue( generator = "increment" )
		@GenericGenerator( name = "increment", strategy = "increment" )
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Instant getDateCreated() {
			return dateCreated;
		}

		public void setDateCreated(Instant dateCreated) {
			this.dateCreated = dateCreated;
		}
	}
}

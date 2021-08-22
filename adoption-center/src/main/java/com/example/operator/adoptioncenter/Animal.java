package com.example.operator.adoptioncenter;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

public class Animal {

	private String name;

	private String namespace;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private Date dateOfBirth;

	private String description;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "Animal{" +
				"name='" + name + '\'' +
				", namespace='" + namespace + '\'' +
				", dateOfBirth=" + dateOfBirth +
				", description='" + description + '\'' +
				'}';
	}
}
